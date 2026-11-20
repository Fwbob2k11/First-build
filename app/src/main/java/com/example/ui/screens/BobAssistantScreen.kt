package com.example.ui.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppTab
import com.example.data.BobAccentColor
import com.example.data.BobAccessory
import com.example.data.BobAppearance
import com.example.data.BobChatState
import com.example.data.BobEyeStyle
import com.example.data.BobMessage
import com.example.data.BobQuickAction
import com.example.data.BobShellStyle
import com.example.ui.components.BobAvatar
import com.example.ui.theme.LocalPulseStyle
import com.example.ui.theme.pulseCardBorder
import com.example.ui.theme.pulseCardContainerColor

private val QuickDiagnosticPrompts = listOf(
    "Why is my device running slow right now?",
    "Explain what my temperature target does",
    "How do I free up storage safely?",
    "Explain all PulseCore features"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BobAssistantScreen(
    isWideLayout: Boolean,
    appearance: BobAppearance,
    chatState: BobChatState,
    onUpdateAppearance: (BobAppearance) -> Unit,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    onNavigateToTab: (AppTab) -> Unit,
    onRunBoost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by rememberSaveable { mutableStateOf("") }
    var showCustomizer by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(chatState.messages.size, chatState.messages.lastOrNull()?.text) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.lastIndex)
        }
    }

    if (showCustomizer) {
        ModalBottomSheet(
            onDismissRequest = { showCustomizer = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BobCustomizerSheetContent(
                appearance = appearance,
                onUpdateAppearance = onUpdateAppearance,
                onDone = { showCustomizer = false }
            )
        }
    }

    val handleQuickAction: (BobQuickAction) -> Unit = { action ->
        when (action) {
            BobQuickAction.RUN_BOOST -> {
                onRunBoost()
                onNavigateToTab(AppTab.BOOST)
            }
            BobQuickAction.OPEN_THERMAL -> onNavigateToTab(AppTab.BOOST)
            BobQuickAction.OPEN_MEMORY -> onNavigateToTab(AppTab.MEMORY)
            BobQuickAction.OPEN_STORAGE -> onNavigateToTab(AppTab.STORAGE)
            BobQuickAction.OPEN_SYSTEM_APPS -> launchSafeSettings(context, Settings.ACTION_APPLICATION_SETTINGS)
        }
    }

    if (isWideLayout) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BobStageCard(
                    appearance = appearance,
                    isGenerating = chatState.isGenerating,
                    onCustomize = { showCustomizer = true },
                    onClearChat = onClearChat
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickPromptStrip(
                    enabled = !chatState.isGenerating,
                    onPickPrompt = onSendMessage
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = chatState.messages, key = { it.id }) { msg ->
                        BobChatBubble(
                            message = msg,
                            appearance = appearance,
                            onActionClick = handleQuickAction
                        )
                    }
                }

                chatState.errorMessage?.let { err ->
                    ErrorBanner(err)
                }

                BobChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    isGenerating = chatState.isGenerating,
                    onSend = {
                        val toSend = inputText
                        inputText = ""
                        onSendMessage(toSend)
                    }
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BobStageCard(
                appearance = appearance,
                isGenerating = chatState.isGenerating,
                onCustomize = { showCustomizer = true },
                onClearChat = onClearChat
            )

            QuickPromptStrip(
                enabled = !chatState.isGenerating,
                onPickPrompt = onSendMessage
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = chatState.messages, key = { it.id }) { msg ->
                    BobChatBubble(
                        message = msg,
                        appearance = appearance,
                        onActionClick = handleQuickAction
                    )
                }
            }

            chatState.errorMessage?.let { err ->
                ErrorBanner(err)
            }

            BobChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                isGenerating = chatState.isGenerating,
                onSend = {
                    val toSend = inputText
                    inputText = ""
                    onSendMessage(toSend)
                }
            )
        }
    }
}

@Composable
private fun BobStageCard(
    appearance: BobAppearance,
    isGenerating: Boolean,
    onCustomize: () -> Unit,
    onClearChat: () -> Unit
) {
    val style = LocalPulseStyle.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = style.cardShape,
        colors = CardDefaults.cardColors(containerColor = pulseCardContainerColor()),
        border = pulseCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BobAvatar(
                appearance = appearance,
                isActive = isGenerating,
                modifier = Modifier.size(76.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bob • Device Helper",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${appearance.shellStyle.label} • ${appearance.accessory.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onCustomize,
                        modifier = Modifier.testTag("customize_bob_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Bob")
                    }

                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_bob_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Reset Bob chat"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPromptStrip(
    enabled: Boolean,
    onPickPrompt: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickDiagnosticPrompts.forEachIndexed { index, prompt ->
            AssistChip(
                onClick = { if (enabled) onPickPrompt(prompt) },
                enabled = enabled,
                label = { Text(prompt) },
                modifier = Modifier.testTag("bob_quick_chip_$index")
            )
        }
    }
}

@Composable
private fun BobChatBubble(
    message: BobMessage,
    appearance: BobAppearance,
    onActionClick: (BobQuickAction) -> Unit
) {
    val isUser = message.isFromUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            BobAvatar(
                appearance = appearance,
                isActive = message.isStreaming,
                modifier = Modifier
                    .padding(end = 8.dp, top = 2.dp)
                    .size(34.dp)
            )
        }

        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 6.dp,
                topEnd = if (isUser) 6.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            }
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (message.isStreaming && message.text.isEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.width(120.dp))
                    Text(
                        text = "Bob is checking your device telemetry...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                if (!isUser && !message.isStreaming && message.suggestedAction != null) {
                    FilledTonalButton(
                        onClick = { onActionClick(message.suggestedAction) },
                        modifier = Modifier.testTag("bob_action_${message.suggestedAction.name.lowercase()}")
                    ) {
                        Text(message.suggestedAction.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun BobChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Ask Bob why your device is slow or how a tool works...") },
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("bob_input_field")
        )

        FilledIconButton(
            onClick = onSend,
            enabled = text.isNotBlank() && !isGenerating,
            modifier = Modifier
                .size(52.dp)
                .testTag("bob_send_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message to Bob"
            )
        }
    }
}

@Composable
private fun BobCustomizerSheetContent(
    appearance: BobAppearance,
    onUpdateAppearance: (BobAppearance) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BobAvatar(
                appearance = appearance,
                isActive = true,
                modifier = Modifier.size(92.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bob Appearance Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize Bob's chassis, color, optics, and gear. Saved automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        CustomizerSectionTitle("Chassis Style")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BobShellStyle.entries.forEach { shell ->
                FilterChip(
                    selected = appearance.shellStyle == shell,
                    onClick = { onUpdateAppearance(appearance.copy(shellStyle = shell)) },
                    label = { Text(shell.label) },
                    modifier = Modifier.testTag("bob_shell_${shell.name.lowercase()}")
                )
            }
        }

        CustomizerSectionTitle("Core Accent Color")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BobAccentColor.entries.forEach { colorOption ->
                val isSelected = appearance.accentColor == colorOption
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(colorOption.colorHex))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                Color.White.copy(alpha = 0.35f)
                            },
                            shape = CircleShape
                        )
                        .clickable {
                            onUpdateAppearance(appearance.copy(accentColor = colorOption))
                        }
                        .testTag("bob_color_${colorOption.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "${colorOption.label} selected",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        CustomizerSectionTitle("Optics / Eyes")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BobEyeStyle.entries.forEach { eye ->
                FilterChip(
                    selected = appearance.eyeStyle == eye,
                    onClick = { onUpdateAppearance(appearance.copy(eyeStyle = eye)) },
                    label = { Text(eye.label) },
                    modifier = Modifier.testTag("bob_eyes_${eye.name.lowercase()}")
                )
            }
        }

        CustomizerSectionTitle("Gear & Accessory")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BobAccessory.entries.forEach { acc ->
                FilterChip(
                    selected = appearance.accessory == acc,
                    onClick = { onUpdateAppearance(appearance.copy(accessory = acc)) },
                    label = { Text(acc.label) },
                    modifier = Modifier.testTag("bob_acc_${acc.name.lowercase()}")
                )
            }
        }

        FilledTonalButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("bob_customizer_done_button")
        ) {
            Text("Save Look")
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CustomizerSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}
