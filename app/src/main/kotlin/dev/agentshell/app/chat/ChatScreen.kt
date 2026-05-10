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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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

    // Auto-scroll to bottom when messages change
    LaunchedEffect(state.messages.size, state.streamingToken) {
        val count = state.messages.size
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgentShellColors.TermBg)
            .imePadding()
    ) {
        // Header bar — matches PRD "path-style" label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(AgentShellColors.Shell1)
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                            content = state.streamingToken + "█" // blinking cursor visual
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

        // Input bar
        ShellInput(
            value = state.currentInput,
            onValueChange = { viewModel.onIntent(ChatIntent.InputChanged(it)) },
            placeholder = if (state.isAgentRunning) "Agent running..." else "Enter task for agent...",
            onSubmit = { viewModel.onIntent(ChatIntent.SubmitTask) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

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
