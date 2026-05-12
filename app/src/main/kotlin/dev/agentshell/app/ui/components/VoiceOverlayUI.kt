package dev.agentshell.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.agentshell.app.agent.AgentState
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

@Composable
fun VoiceOverlayUI(
    agentState: AgentState,
    spokenText: String,
    modifier: Modifier = Modifier
) {
    // Keep track of the last 4 messages
    val messages = remember { mutableStateListOf<String>() }

    LaunchedEffect(agentState, spokenText) {
        val newMessage = when {
            spokenText.isNotEmpty() && agentState == AgentState.Idle -> "User: $spokenText"
            agentState is AgentState.Planning -> "Planning: ${agentState.task}"
            agentState is AgentState.Thinking -> "Thinking (Step ${agentState.step}/${agentState.maxSteps})..."
            agentState is AgentState.Acting -> "Tool: ${agentState.tool}"
            agentState is AgentState.Reflecting -> "Reflecting on outcome..."
            agentState is AgentState.Streaming -> "Thinking..." // We can just show 'Thinking...' instead of fast streaming text to keep it minimal
            else -> ""
        }

        if (newMessage.isNotEmpty()) {
            if (messages.isEmpty() || messages.last() != newMessage) {
                messages.add(newMessage)
                if (messages.size > 4) {
                    messages.removeAt(0)
                }
            }
        }
    }

    // If idle and no spoken text, maybe clear after a delay, or just hide
    LaunchedEffect(agentState) {
        if (agentState == AgentState.Idle) {
            kotlinx.coroutines.delay(3000)
            messages.clear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        messages.reversed().forEachIndexed { index, msg ->
            val alphaValue = 1f - (index * 0.25f)
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 })
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .alpha(alphaValue.coerceIn(0f, 1f))
                        .background(AgentShellColors.Shell1, RoundedCornerShape(12.dp))
                        .border(1.dp, AgentShellColors.Amber.copy(alpha = alphaValue.coerceIn(0f, 1f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        color = AgentShellColors.Amber,
                        style = AgentShellTypography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
