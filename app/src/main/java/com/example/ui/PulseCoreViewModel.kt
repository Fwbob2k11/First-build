package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppTab
import com.example.data.BackgroundMediaRepository
import com.example.data.BackgroundType
import com.example.data.BobAppearance
import com.example.data.BobChatState
import com.example.data.BobMessage
import com.example.data.BobQuickAction
import com.example.data.BoostReport
import com.example.data.BoostSectionId
import com.example.data.DeviceProfile
import com.example.data.DeviceTelemetryRepository
import com.example.data.LiveTelemetry
import com.example.data.PreferencesRepository
import com.example.data.PulseCorePreferences
import com.example.data.ScannedFileItem
import com.example.data.ScannedFolder
import com.example.data.StorageCategory
import com.example.data.StorageRepository
import com.example.data.StorageScanState
import com.example.data.StyleConfig
import com.example.data.ThermalAiRepository
import com.example.data.ThermalAiState
import com.example.data.formatBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private const val TAG = "PulseCoreViewModel"

class PulseCoreViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepo = PreferencesRepository(application)
    private val telemetryRepo = DeviceTelemetryRepository(application)
    private val storageRepo = StorageRepository(application)
    private val backgroundRepo = BackgroundMediaRepository(application)
    private val aiRepo = ThermalAiRepository()

    val deviceProfile: DeviceProfile = telemetryRepo.getDeviceProfile()

    private val _selectedTab = MutableStateFlow(AppTab.BOOST)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _preferences = MutableStateFlow(PulseCorePreferences())
    val preferences: StateFlow<PulseCorePreferences> = _preferences.asStateFlow()

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting.asStateFlow()

    private val _lastBoostReport = MutableStateFlow<BoostReport?>(null)
    val lastBoostReport: StateFlow<BoostReport?> = _lastBoostReport.asStateFlow()

    private val _thermalAiState = MutableStateFlow<ThermalAiState>(ThermalAiState.Idle)
    val thermalAiState: StateFlow<ThermalAiState> = _thermalAiState.asStateFlow()

    private val _scannedFolders = MutableStateFlow<List<ScannedFolder>>(emptyList())
    val scannedFolders: StateFlow<List<ScannedFolder>> = _scannedFolders.asStateFlow()

    private val _scannedFiles = MutableStateFlow<List<ScannedFileItem>>(emptyList())
    val scannedFiles: StateFlow<List<ScannedFileItem>> = _scannedFiles.asStateFlow()

    private val _storageFilter = MutableStateFlow(StorageCategory.ALL)
    val storageFilter: StateFlow<StorageCategory> = _storageFilter.asStateFlow()

    private val _selectedFileUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedFileUris: StateFlow<Set<String>> = _selectedFileUris.asStateFlow()

    private val _storageScanState = MutableStateFlow<StorageScanState>(StorageScanState.Idle)
    val storageScanState: StateFlow<StorageScanState> = _storageScanState.asStateFlow()

    private val _storageStatusBanner = MutableStateFlow<String?>(null)
    val storageStatusBanner: StateFlow<String?> = _storageStatusBanner.asStateFlow()

    private val _bobChatState = MutableStateFlow(
        BobChatState(
            messages = listOf(createWelcomeBobMessage())
        )
    )
    val bobChatState: StateFlow<BobChatState> = _bobChatState.asStateFlow()

    private var currentFps: Float = 60.0f
    private var currentFrameMs: Float = 16.6f
    private val fpsSamples = ArrayDeque<Float>()

    private var thermalAiJob: Job? = null
    private var bobAiJob: Job? = null
    private var initialThermalTriggered = false

    init {
        viewModelScope.launch {
            preferencesRepo.preferencesFlow.collect { prefs ->
                val previousFolders = _preferences.value.authorizedFolderUris
                _preferences.value = prefs
                if (prefs.authorizedFolderUris != previousFolders && prefs.authorizedFolderUris.isNotEmpty()) {
                    scanAuthorizedFolders(prefs.authorizedFolderUris)
                }
            }
        }

        viewModelScope.launch {
            val initialPrefs = preferencesRepo.preferencesFlow.first()
            _preferences.value = initialPrefs
            refreshTelemetry()
            if (initialPrefs.authorizedFolderUris.isNotEmpty()) {
                scanAuthorizedFolders(initialPrefs.authorizedFolderUris)
            }
            if (!initialThermalTriggered) {
                initialThermalTriggered = true
                requestThermalOverview(initialPrefs.targetTempC)
            }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(2_000L)
                refreshTelemetry()
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun onFrameMetricsSample(fps: Float, frameTimeMs: Float) {
        currentFps = fps.coerceIn(1f, 240f)
        currentFrameMs = frameTimeMs.coerceIn(1f, 250f)
        if (fpsSamples.size >= 24) {
            fpsSamples.removeFirst()
        }
        fpsSamples.addLast(currentFps)
        _telemetry.update {
            it.copy(
                measuredFps = currentFps,
                frameTimeMs = currentFrameMs,
                fpsHistory = fpsSamples.toList()
            )
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            val snapshot = telemetryRepo.readLiveTelemetry(
                measuredFps = currentFps,
                frameTimeMs = currentFrameMs,
                fpsHistory = fpsSamples.toList()
            )
            _telemetry.value = snapshot
        }
    }

    fun onTargetTempSliderChanged(newTempC: Float) {
        val snapped = (kotlin.math.round(newTempC * 2f) / 2f).coerceIn(34.0f, 45.0f)
        _preferences.update { it.copy(targetTempC = snapped) }
    }

    fun onTargetTempCommitted() {
        val committed = _preferences.value.targetTempC
        viewModelScope.launch {
            preferencesRepo.setTargetTempC(committed)
            requestThermalOverview(committed)
        }
    }

    fun refreshThermalOverviewNow() {
        requestThermalOverview(_preferences.value.targetTempC)
    }

    private fun requestThermalOverview(targetTempC: Float) {
        thermalAiJob?.cancel()
        thermalAiJob = viewModelScope.launch {
            _thermalAiState.value = ThermalAiState.Loading
            val accumulated = StringBuilder()
            try {
                aiRepo.streamThermalOverview(
                    profile = deviceProfile,
                    telemetry = _telemetry.value,
                    targetTempC = targetTempC,
                    autoFrameGuard = _preferences.value.autoFrameGuard
                ).collect { chunk ->
                    accumulated.append(chunk)
                    _thermalAiState.value = ThermalAiState.Streaming(
                        text = accumulated.toString(),
                        targetTempC = targetTempC
                    )
                }
                if (accumulated.isNotBlank()) {
                    _thermalAiState.value = ThermalAiState.Success(
                        text = accumulated.toString().trim(),
                        targetTempC = targetTempC
                    )
                } else {
                    _thermalAiState.value = ThermalAiState.Error(
                        "Gemini returned an empty thermal overview. Tap Refresh to retry."
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Thermal AI overview timed out", e)
                _thermalAiState.value = ThermalAiState.Error(
                    "Thermal AI overview timed out after 60 seconds. Please check connection and retry."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Thermal AI overview failed", e)
                _thermalAiState.value = ThermalAiState.Error(
                    e.localizedMessage ?: "Unable to reach Firebase AI Logic right now."
                )
            }
        }
    }

    fun toggleAutoFrameGuard(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setAutoFrameGuard(enabled)
        }
    }

    fun toggleTemperatureUnit(useFahrenheit: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setUseFahrenheit(useFahrenheit)
        }
    }

    fun runGamingBoost() {
        if (_isBoosting.value) return
        viewModelScope.launch {
            _isBoosting.value = true
            try {
                // Brief pacing step so telemetry sampling settles cleanly after trim
                val report = telemetryRepo.performMemoryAndCacheTrim(
                    autoFrameGuardActive = _preferences.value.autoFrameGuard
                )
                _lastBoostReport.value = report
                refreshTelemetry()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Boost execution failed", e)
            } finally {
                _isBoosting.value = false
            }
        }
    }

    fun onFolderPicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val granted = storageRepo.takePersistablePermission(uri)
            if (granted) {
                preferencesRepo.addAuthorizedFolderUri(uri.toString())
                _storageStatusBanner.value = "Folder linked. Scanning files..."
                val updated = _preferences.value.authorizedFolderUris + uri.toString()
                scanAuthorizedFolders(updated)
            } else {
                _storageScanState.value = StorageScanState.Error(
                    "Could not retain folder permission. Please select the folder again."
                )
            }
        }
    }

    fun removeFolder(uriString: String) {
        viewModelScope.launch {
            storageRepo.releasePersistablePermission(uriString)
            preferencesRepo.removeAuthorizedFolderUri(uriString)
            _selectedFileUris.update { selected ->
                val remainingFileUris = _scannedFiles.value
                    .filter { it.folderUriString != uriString }
                    .map { it.uriString }
                    .toSet()
                selected.intersect(remainingFileUris)
            }
            val remaining = _preferences.value.authorizedFolderUris - uriString
            if (remaining.isEmpty()) {
                _scannedFolders.value = emptyList()
                _scannedFiles.value = emptyList()
                _storageScanState.value = StorageScanState.Idle
            } else {
                scanAuthorizedFolders(remaining)
            }
        }
    }

    fun rescanAuthorizedFolders() {
        val uris = _preferences.value.authorizedFolderUris
        if (uris.isNotEmpty()) {
            scanAuthorizedFolders(uris)
        }
    }

    private fun scanAuthorizedFolders(uris: Set<String>) {
        viewModelScope.launch {
            _storageScanState.value = StorageScanState.Scanning("Authorized folders", 0)
            try {
                val result = storageRepo.scanAuthorizedFolders(uris) { folderName, count ->
                    _storageScanState.value = StorageScanState.Scanning(folderName, count)
                }
                _scannedFolders.value = result.folders
                _scannedFiles.value = result.files
                _selectedFileUris.update { current ->
                    val validUris = result.files.map { it.uriString }.toSet()
                    current.intersect(validUris)
                }
                _storageScanState.value = StorageScanState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Folder scan failed", e)
                _storageScanState.value = StorageScanState.Error(
                    e.localizedMessage ?: "Failed to scan selected folders."
                )
            }
        }
    }

    fun setStorageFilter(category: StorageCategory) {
        _storageFilter.value = category
    }

    fun toggleFileSelection(uriString: String) {
        _selectedFileUris.update { current ->
            if (uriString in current) current - uriString else current + uriString
        }
    }

    fun selectAllFilteredFiles(filteredItems: List<ScannedFileItem>) {
        val uris = filteredItems.map { it.uriString }.toSet()
        _selectedFileUris.update { current ->
            if (uris.isNotEmpty() && uris.all { it in current }) {
                current - uris
            } else {
                current + uris
            }
        }
    }

    fun deleteSelectedFiles() {
        val selected = _selectedFileUris.value
        if (selected.isEmpty()) return
        val items = _scannedFiles.value.filter { it.uriString in selected }
        viewModelScope.launch {
            val result = storageRepo.deleteFiles(items)
            _selectedFileUris.value = emptySet()
            _storageStatusBanner.value = buildString {
                append("Deleted ${result.deletedCount} file(s), freed ${formatBytes(result.freedBytes)}")
                if (result.failedCount > 0) {
                    append(" (${result.failedCount} could not be removed by OS provider)")
                }
            }
            refreshTelemetry()
            rescanAuthorizedFolders()
        }
    }

    fun clearAppCacheOnly() {
        viewModelScope.launch {
            val report = telemetryRepo.performMemoryAndCacheTrim(_preferences.value.autoFrameGuard)
            _lastBoostReport.value = report
            _storageStatusBanner.value = if (report.freedCacheBytes > 0L) {
                "Cleared ${formatBytes(report.freedCacheBytes)} of PulseCore cache."
            } else {
                "PulseCore internal cache is already clean."
            }
            refreshTelemetry()
        }
    }

    fun dismissStorageBanner() {
        _storageStatusBanner.value = null
    }

    fun updateBobAppearance(appearance: BobAppearance) {
        _preferences.update { it.copy(bobAppearance = appearance) }
        viewModelScope.launch {
            preferencesRepo.saveBobAppearance(appearance)
        }
    }

    fun updateStyleConfig(transform: (StyleConfig) -> StyleConfig) {
        val updated = transform(_preferences.value.styleConfig)
        _preferences.update { it.copy(styleConfig = updated) }
        viewModelScope.launch {
            preferencesRepo.saveStyleConfig(updated)
        }
    }

    fun moveBoostSection(section: BoostSectionId, moveUp: Boolean) {
        updateStyleConfig { current ->
            val list = current.boostCardOrder.toMutableList()
            val index = list.indexOf(section)
            if (index == -1) return@updateStyleConfig current
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in list.indices) return@updateStyleConfig current
            list[index] = list[targetIndex]
            list[targetIndex] = section
            current.copy(boostCardOrder = list)
        }
    }

    fun toggleBoostSectionVisibility(section: BoostSectionId) {
        updateStyleConfig { current ->
            val hidden = current.hiddenBoostSections.toMutableSet()
            if (section in hidden) {
                hidden.remove(section)
            } else {
                val visibleCount = current.boostCardOrder.count { it !in hidden }
                if (visibleCount > 1) {
                    hidden.add(section)
                }
            }
            current.copy(hiddenBoostSections = hidden)
        }
    }

    fun importCustomBackgroundPhoto(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val savedPath = backgroundRepo.importCustomBackgroundPhoto(uri) ?: return@launch
            updateStyleConfig { current ->
                current.copy(
                    backgroundConfig = current.backgroundConfig.copy(
                        backgroundType = BackgroundType.CUSTOM_PHOTO,
                        customPhotoPath = savedPath,
                        photoRevision = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun clearCustomBackgroundPhoto() {
        viewModelScope.launch {
            backgroundRepo.clearCustomBackgroundPhoto()
            updateStyleConfig { current ->
                current.copy(
                    backgroundConfig = current.backgroundConfig.copy(
                        backgroundType = BackgroundType.ADAPTIVE,
                        customPhotoPath = null,
                        photoRevision = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun resetStyleToDefaults() {
        viewModelScope.launch {
            backgroundRepo.clearCustomBackgroundPhoto()
            val defaults = StyleConfig()
            _preferences.update { it.copy(styleConfig = defaults) }
            preferencesRepo.saveStyleConfig(defaults)
        }
    }

    fun sendBobMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _bobChatState.value.isGenerating) return

        val userMsg = BobMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = true,
            text = prompt,
            timestampMs = System.currentTimeMillis()
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderBotMsg = BobMessage(
            id = assistantMsgId,
            isFromUser = false,
            text = "",
            timestampMs = System.currentTimeMillis(),
            isStreaming = true,
            suggestedAction = inferSuggestedAction(prompt)
        )

        val priorHistory = _bobChatState.value.messages
        _bobChatState.update {
            it.copy(
                messages = it.messages + userMsg + placeholderBotMsg,
                isGenerating = true,
                errorMessage = null
            )
        }

        bobAiJob?.cancel()
        bobAiJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                aiRepo.streamBobReply(
                    userMessage = prompt,
                    priorHistory = priorHistory,
                    profile = deviceProfile,
                    telemetry = _telemetry.value,
                    targetTempC = _preferences.value.targetTempC,
                    autoFrameGuard = _preferences.value.autoFrameGuard,
                    authorizedFolderCount = _preferences.value.authorizedFolderUris.size
                ).collect { chunk ->
                    accumulated.append(chunk)
                    val currentText = accumulated.toString()
                    _bobChatState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMsgId) {
                                    msg.copy(text = currentText, isStreaming = true)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }

                val finalText = accumulated.toString().trim()
                if (finalText.isNotEmpty()) {
                    _bobChatState.update { state ->
                        state.copy(
                            isGenerating = false,
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMsgId) {
                                    msg.copy(text = finalText, isStreaming = false)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                } else {
                    _bobChatState.update { state ->
                        state.copy(
                            isGenerating = false,
                            errorMessage = "Bob received an empty response from Gemini.",
                            messages = state.messages.filterNot { it.id == assistantMsgId }
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Bob AI response timed out", e)
                _bobChatState.update { state ->
                    state.copy(
                        isGenerating = false,
                        errorMessage = "Bob timed out after 60 seconds. Please try asking again.",
                        messages = state.messages.filterNot { it.id == assistantMsgId }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Bob AI failed", e)
                _bobChatState.update { state ->
                    state.copy(
                        isGenerating = false,
                        errorMessage = e.localizedMessage ?: "Bob couldn't connect to Firebase AI Logic.",
                        messages = state.messages.filterNot { it.id == assistantMsgId }
                    )
                }
            }
        }
    }

    fun clearBobChat() {
        bobAiJob?.cancel()
        _bobChatState.value = BobChatState(
            messages = listOf(createWelcomeBobMessage())
        )
    }

    private fun inferSuggestedAction(prompt: String): BobQuickAction? {
        val lower = prompt.lowercase(Locale.US)
        return when {
            "storage" in lower || "space" in lower || "file" in lower || "folder" in lower ->
                BobQuickAction.OPEN_STORAGE
            "ram" in lower || "memory" in lower || "background" in lower || "app" in lower ->
                BobQuickAction.OPEN_MEMORY
            "temp" in lower || "hot" in lower || "heat" in lower || "thermal" in lower ->
                BobQuickAction.OPEN_THERMAL
            "slow" in lower || "lag" in lower || "fps" in lower || "game" in lower || "boost" in lower ->
                BobQuickAction.RUN_BOOST
            else -> null
        }
    }

    private fun createWelcomeBobMessage(): BobMessage {
        val deviceName = deviceProfile.brandDisplay
        val form = if (deviceProfile.isTablet) "tablet" else "phone"
        return BobMessage(
            id = "bob_welcome",
            isFromUser = false,
            text = "Hey! I'm Bob, your PulseCore sidekick on your $deviceName ($form). " +
                "Customize my look anytime above, or ask me how any tool works or why your device feels slow, warm, or low on storage.",
            timestampMs = System.currentTimeMillis(),
            suggestedAction = BobQuickAction.RUN_BOOST
        )
    }
}
