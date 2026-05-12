package dev.agentshell.app.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agentshell.app.ui.components.ShellInput
import dev.agentshell.app.ui.components.ShellPanel
import dev.agentshell.app.ui.components.Spacing
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var drawerOpen by remember { mutableStateOf(false) }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(state.messages.size, state.streamingToken) {
        val count = state.messages.size
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ─── Main chat layout ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AgentShellColors.TermBg)
                .imePadding()
        ) {
            // Header bar — ☰ hamburger + path label + agent indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(AgentShellColors.Shell1)
                    .padding(horizontal = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger button
                Text(
                    text = "☰",
                    color = AgentShellColors.Amber,
                    style = AgentShellTypography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { drawerOpen = true }
                        .padding(end = 10.dp)
                )
                Text(
                    text = "[SYS] / CHAT // AGENT LOOP",
                    color = AgentShellColors.Amber,
                    style = AgentShellTypography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.isAgentRunning) {
                    AgentThinkingIndicator()
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatMessageRow(message)
                }

                // Live streaming token preview
                if (state.streamingToken.isNotEmpty()) {
                    item(key = "streaming") {
                        ChatMessageRow(
                            message = ChatMessage(
                                role = MessageRole.STREAMING,
                                content = state.streamingToken + "█"
                            )
                        )
                    }
                }
            }

            // Agent running status bar
            AnimatedVisibility(
                visible = state.isAgentRunning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AgentShellColors.Shell2)
                        .padding(horizontal = Spacing.lg, vertical = 4.dp)
                ) {
                    Text(
                        text = "▶ AGENT RUNNING — PLAN → ACT → OBSERVE",
                        color = AgentShellColors.TermSys,
                        style = AgentShellTypography.labelSmall
                    )
                }
            }

            // Suggestion Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                val suggestions = listOf(
                    "Create a Calculator Mini-App",
                    "Create a Weather Mini-App",
                    "Create a To-Do List Mini-App",
                    "Run a full Termux system update",
                    "Clear the terminal screen"
                )
                items(suggestions) { suggestion ->
                    Box(
                        modifier = Modifier
                            .background(AgentShellColors.Shell1)
                            .clickable {
                                viewModel.onIntent(ChatIntent.InputChanged(suggestion))
                                viewModel.onIntent(ChatIntent.SubmitTask)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = AgentShellColors.Text1,
                            style = AgentShellTypography.labelSmall
                        )
                    }
                }
            }

            // Input bar + Mic Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgentShellColors.Shell0)
                    .padding(end = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShellInput(
                    value = if (state.isListening && state.liveVoiceText.isNotEmpty()) state.liveVoiceText else state.currentInput,
                    onValueChange = { viewModel.onIntent(ChatIntent.InputChanged(it)) },
                    placeholder = if (state.isListening) "Listening..." else if (state.isAgentRunning) "Agent running..." else "Enter task or hold mic...",
                    onSubmit = { viewModel.onIntent(ChatIntent.SubmitTask) },
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (state.isListening) AgentShellColors.Amber else AgentShellColors.Shell2)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.onIntent(ChatIntent.StartVoiceInput)
                                    tryAwaitRelease()
                                    viewModel.onIntent(ChatIntent.StopVoiceInput)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎤",
                        color = if (state.isListening) AgentShellColors.TermBg else AgentShellColors.Text1,
                        style = AgentShellTypography.titleMedium
                    )
                }
            }
        }

        // ─── Drawer scrim (tap-outside to close) ─────────────────────────────
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { drawerOpen = false }
            )
        }

        // ─── Slide-in drawer panel ────────────────────────────────────────────
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally(tween(250)) { -it },
            exit = slideOutHorizontally(tween(200)) { -it }
        ) {
            SessionDrawer(
                onClose = { drawerOpen = false },
                onNewSession = {
                    viewModel.onIntent(ChatIntent.NewSession)
                    drawerOpen = false
                },
                onClearChat = {
                    viewModel.onIntent(ChatIntent.ClearChat)
                    drawerOpen = false
                }
            )
        }
    }
}

// ─── Session Drawer ───────────────────────────────────────────────────────────

@Composable
private fun SessionDrawer(
    onClose: () -> Unit,
    onNewSession: () -> Unit,
    onClearChat: () -> Unit
) {
    // Past sessions — stub list until Room persistence is wired
    val pastSessions: List<String> = emptyList()

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 300.dp)
            .fillMaxWidth(0.82f)
            .background(AgentShellColors.Shell0)
            // Consume clicks so they don't fall through to the scrim
            .clickable(enabled = false) {}
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Drawer header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgentShellColors.Shell1)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "// SESSIONS",
                    color = AgentShellColors.Amber,
                    style = AgentShellTypography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // ✕ Close button
                Text(
                    text = "✕",
                    color = AgentShellColors.Text3,
                    style = AgentShellTypography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action buttons ────────────────────────────────────────────────
            DrawerActionRow(
                icon = "+",
                label = "NEW SESSION",
                color = AgentShellColors.TermCmd,
                onClick = onNewSession
            )

            DrawerActionRow(
                icon = "⌫",
                label = "CLEAR CURRENT CHAT",
                color = AgentShellColors.Text2,
                onClick = onClearChat
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = AgentShellColors.Shell2, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // ── Past sessions list ────────────────────────────────────────────
            Text(
                text = "PAST SESSIONS",
                color = AgentShellColors.Text3,
                style = AgentShellTypography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (pastSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No previous sessions.\nStart a new one above.",
                        color = AgentShellColors.Text3,
                        style = AgentShellTypography.bodySmall
                    )
                }
            } else {
                LazyColumn {
                    items(pastSessions) { session ->
                        Text(
                            text = session,
                            color = AgentShellColors.Text1,
                            style = AgentShellTypography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* TODO: load session */ }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                        Divider(color = AgentShellColors.Shell2, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            color = color,
            style = AgentShellTypography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = color,
            style = AgentShellTypography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Chat message row ─────────────────────────────────────────────────────────

@Composable
private fun ChatMessageRow(message: ChatMessage) {
    val (color, prefix) = when (message.role) {
        MessageRole.USER        -> AgentShellColors.Text0 to "> "
        MessageRole.AGENT       -> AgentShellColors.Text1 to "  "
        MessageRole.STREAMING   -> AgentShellColors.Amber to "  "
        MessageRole.TOOL_CALL   -> AgentShellColors.TermSys to "  "
        MessageRole.TOOL_OUTPUT -> AgentShellColors.TermOut to "  │ "
        MessageRole.SYSTEM      -> AgentShellColors.Info to "  "
        MessageRole.ERROR       -> AgentShellColors.Error to "  "
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$prefix${message.content}",
            color = color,
            style = AgentShellTypography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Agent thinking indicator ─────────────────────────────────────────────────

@Composable
private fun AgentThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Text(
        text = "●",
        color = AgentShellColors.Amber,
        style = AgentShellTypography.bodySmall,
        modifier = Modifier.alpha(alpha)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
        text = "THINKING",
        color = AgentShellColors.Text3,
        style = AgentShellTypography.labelSmall
    )
}
