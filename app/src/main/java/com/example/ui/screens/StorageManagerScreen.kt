package com.example.ui.screens

import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LiveTelemetry
import com.example.data.ScannedFileItem
import com.example.data.ScannedFolder
import com.example.data.StorageCategory
import com.example.data.StorageScanState
import com.example.data.formatBytes
import com.example.ui.components.ArcMetricGauge
import com.example.ui.theme.LocalPulseStyle
import com.example.ui.theme.pulseCardBorder
import com.example.ui.theme.pulseCardContainerColor
import java.text.DateFormat
import java.util.Date

@Composable
fun StorageManagerScreen(
    isWideLayout: Boolean,
    telemetry: LiveTelemetry,
    scannedFolders: List<ScannedFolder>,
    scannedFiles: List<ScannedFileItem>,
    activeFilter: StorageCategory,
    selectedUris: Set<String>,
    scanState: StorageScanState,
    statusBanner: String?,
    onFolderPicked: (Uri?) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onRescan: () -> Unit,
    onSelectFilter: (StorageCategory) -> Unit,
    onToggleFileSelection: (String) -> Unit,
    onSelectAllFiltered: (List<ScannedFileItem>) -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAppCache: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = onFolderPicked
    )

    val filteredFiles = remember(scannedFiles, activeFilter) {
        when (activeFilter) {
            StorageCategory.ALL -> scannedFiles
            else -> scannedFiles.filter { it.category == activeFilter }
        }
    }

    val selectedBytes = remember(scannedFiles, selectedUris) {
        scannedFiles.filter { it.uriString in selectedUris }.sumOf { it.sizeBytes }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedUris.size} file(s)?") },
            text = {
                Text(
                    "This permanently removes the selected files (${formatBytes(selectedBytes)}) " +
                        "from your device storage. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_dialog_button")
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isWideLayout) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    VolumeOverviewCard(
                        telemetry = telemetry,
                        onClearAppCache = onClearAppCache,
                        onOpenSystemStorage = {
                            launchSafeSettings(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                        }
                    )
                }
                item {
                    AuthorizedFoldersCard(
                        scannedFolders = scannedFolders,
                        scanState = scanState,
                        onAddFolder = { folderPickerLauncher.launch(null) },
                        onRescan = onRescan,
                        onRemoveFolder = onRemoveFolder
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StorageFilesHeaderSection(
                    statusBanner = statusBanner,
                    onDismissBanner = onDismissBanner,
                    activeFilter = activeFilter,
                    onSelectFilter = onSelectFilter,
                    filteredFiles = filteredFiles,
                    selectedUris = selectedUris,
                    selectedBytes = selectedBytes,
                    onSelectAllFiltered = { onSelectAllFiltered(filteredFiles) },
                    onRequestDelete = { showDeleteConfirm = true }
                )

                StorageFileListBody(
                    scannedFolders = scannedFolders,
                    filteredFiles = filteredFiles,
                    selectedUris = selectedUris,
                    scanState = scanState,
                    onToggleFile = onToggleFileSelection,
                    onAddFolder = { folderPickerLauncher.launch(null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                VolumeOverviewCard(
                    telemetry = telemetry,
                    onClearAppCache = onClearAppCache,
                    onOpenSystemStorage = {
                        launchSafeSettings(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                    }
                )
            }
            item {
                AuthorizedFoldersCard(
                    scannedFolders = scannedFolders,
                    scanState = scanState,
                    onAddFolder = { folderPickerLauncher.launch(null) },
                    onRescan = onRescan,
                    onRemoveFolder = onRemoveFolder
                )
            }
            item {
                StorageFilesHeaderSection(
                    statusBanner = statusBanner,
                    onDismissBanner = onDismissBanner,
                    activeFilter = activeFilter,
                    onSelectFilter = onSelectFilter,
                    filteredFiles = filteredFiles,
                    selectedUris = selectedUris,
                    selectedBytes = selectedBytes,
                    onSelectAllFiltered = { onSelectAllFiltered(filteredFiles) },
                    onRequestDelete = { showDeleteConfirm = true }
                )
            }

            if (scannedFolders.isEmpty()) {
                item {
                    EmptyFolderCard(onAddFolder = { folderPickerLauncher.launch(null) })
                }
            } else if (filteredFiles.isEmpty() && scanState !is StorageScanState.Scanning) {
                item {
                    EmptyFilterCard()
                }
            } else {
                items(
                    items = filteredFiles,
                    key = { it.uriString }
                ) { file ->
                    FileItemRow(
                        item = file,
                        checked = file.uriString in selectedUris,
                        onToggle = { onToggleFileSelection(file.uriString) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun VolumeOverviewCard(
    telemetry: LiveTelemetry,
    onClearAppCache: () -> Unit,
    onOpenSystemStorage: () -> Unit
) {
    val style = LocalPulseStyle.current
    val usedPct = (telemetry.storageUsageFraction * 100).toInt()
    val gaugeColor = when {
        usedPct >= 90 -> MaterialTheme.colorScheme.error
        usedPct >= 75 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(containerColor = pulseCardContainerColor()),
        border = pulseCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(style.cardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SdStorage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Real Storage Manager",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ArcMetricGauge(
                    progress = telemetry.storageUsageFraction,
                    valueText = "$usedPct%",
                    labelText = "VOLUME USED",
                    accentColor = gaugeColor,
                    gaugeSize = 122.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${formatBytes(telemetry.storageFreeBytes)} Free",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatBytes(telemetry.storageUsedBytes)} used of ${formatBytes(telemetry.storageTotalBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "PulseCore Cache: ${formatBytes(telemetry.appCacheBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onClearAppCache,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_app_cache_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Cache")
                }

                FilledTonalButton(
                    onClick = onOpenSystemStorage,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_system_storage_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OS Storage")
                }
            }
        }
    }
}

@Composable
private fun AuthorizedFoldersCard(
    scannedFolders: List<ScannedFolder>,
    scanState: StorageScanState,
    onAddFolder: () -> Unit,
    onRescan: () -> Unit,
    onRemoveFolder: (String) -> Unit
) {
    val style = LocalPulseStyle.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(containerColor = pulseCardContainerColor()),
        border = pulseCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(style.cardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Authorized Folders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Link Downloads, Movies, or game folders via Android picker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (scannedFolders.isNotEmpty()) {
                    IconButton(
                        onClick = onRescan,
                        modifier = Modifier.testTag("rescan_folders_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan folders"
                        )
                    }
                }
            }

            Button(
                onClick = onAddFolder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_folder_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Folder to Scan")
            }

            if (scanState is StorageScanState.Scanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Scanning ${scanState.folderName} (${scanState.scannedCount} files)...",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (scanState is StorageScanState.Error) {
                Text(
                    text = scanState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (scannedFolders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    scannedFolders.forEach { folder ->
                        InputChip(
                            selected = true,
                            onClick = { },
                            label = {
                                Text(
                                    "${folder.displayName} (${folder.itemCount} • ${formatBytes(folder.totalBytes)})",
                                    maxLines = 1
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Unlink ${folder.displayName}",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onRemoveFolder(folder.uriString) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageFilesHeaderSection(
    statusBanner: String?,
    onDismissBanner: () -> Unit,
    activeFilter: StorageCategory,
    onSelectFilter: (StorageCategory) -> Unit,
    filteredFiles: List<ScannedFileItem>,
    selectedUris: Set<String>,
    selectedBytes: Long,
    onSelectAllFiltered: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(visible = statusBanner != null) {
            statusBanner?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissBanner) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss message",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StorageCategory.entries.forEach { cat ->
                FilterChip(
                    selected = activeFilter == cat,
                    onClick = { onSelectFilter(cat) },
                    label = { Text(cat.label) },
                    modifier = Modifier.testTag("storage_filter_${cat.name.lowercase()}")
                )
            }
        }

        if (filteredFiles.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSelectAllFiltered,
                    modifier = Modifier.testTag("select_all_files_button")
                ) {
                    val allChecked = filteredFiles.all { it.uriString in selectedUris }
                    Text(if (allChecked) "Deselect All" else "Select All (${filteredFiles.size})")
                }

                if (selectedUris.isNotEmpty()) {
                    Button(
                        onClick = onRequestDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("delete_selected_files_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete (${formatBytes(selectedBytes)})")
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageFileListBody(
    scannedFolders: List<ScannedFolder>,
    filteredFiles: List<ScannedFileItem>,
    selectedUris: Set<String>,
    scanState: StorageScanState,
    onToggleFile: (String) -> Unit,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (scannedFolders.isEmpty()) {
        EmptyFolderCard(onAddFolder = onAddFolder)
    } else if (filteredFiles.isEmpty() && scanState !is StorageScanState.Scanning) {
        EmptyFilterCard()
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = filteredFiles, key = { it.uriString }) { item ->
                FileItemRow(
                    item = item,
                    checked = item.uriString in selectedUris,
                    onToggle = { onToggleFile(item.uriString) }
                )
            }
        }
    }
}

@Composable
private fun FileItemRow(
    item: ScannedFileItem,
    checked: Boolean,
    onToggle: () -> Unit
) {
    val dateLabel = remember(item.lastModifiedMs) {
        if (item.lastModifiedMs > 0L) {
            DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.lastModifiedMs))
        } else {
            "Unknown date"
        }
    }

    val style = LocalPulseStyle.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = style.cardShape,
        color = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            pulseCardContainerColor()
        },
        border = pulseCardBorder(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() }
            )
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.category.label} • $dateLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatBytes(item.sizeBytes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyFolderCard(onAddFolder: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No folders linked yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap Add Folder to Scan to safely inspect Downloads, large media, or game archives and delete real files with zero broad permissions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onAddFolder) {
                Text("Choose Folder")
            }
        }
    }
}

@Composable
private fun EmptyFilterCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No files match this filter.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Try switching to All Files or linking another folder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
