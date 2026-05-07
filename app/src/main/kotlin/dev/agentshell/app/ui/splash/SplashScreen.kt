package dev.agentshell.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography
import kotlinx.coroutines.delay

// ASCII logo — 24 chars wide × 12 lines
private val ASCII_LOGO = """
   ____   _____ _   _ _______
  / _  | / ____| | | |__   __|
 | (_| || |  __| |_| |  | |
  > _  || | |_ |  _  |  | |
 | | | || |__| | | | |  | |
 | |_| | \_____|_| |_|  |_|
  \___/  SHELL v1.0
 ─────────────────────────────
  "Your phone. Your agent."
  "No cloud required."
 ─────────────────────────────
""".trimIndent()

private val STATUS_MESSAGES = listOf(
    "LOADING CORE",
    "MOUNTING FS",
    "STARTING LLM",
    "READY"
)

private const val PROGRESS_SEGMENTS = 16
private const val CHAR_DELAY_MS = 3L
private const val PROGRESS_STEP_DELAY_MS = 80L

/**
 * Splash screen per PRD Section 2 (Screen A).
 *
 * Animation sequence:
 *  0ms    → Background fade in #1C0F09
 *  300ms  → Status line appears + blink
 *  600ms  → ASCII art typewriter (char-by-char)
 *  art done → Progress bar fills (16 segments)
 *  READY  → navigates to app home
 */
@Composable
fun SplashScreen(onAnimationComplete: () -> Unit) {
    val bgAlpha = remember { Animatable(0f) }
    var statusText by remember { mutableStateOf("[SYS] / INIT ···") }
    var displayedChars by remember { mutableIntStateOf(0) }
    var progressFill by remember { mutableIntStateOf(0) }
    var statusIndex by remember { mutableIntStateOf(0) }

    val fullLogoText = ASCII_LOGO
    val totalChars = fullLogoText.length

    LaunchedEffect(Unit) {
        // 1. Background fade in (300ms)
        bgAlpha.animateTo(1f, tween(300, easing = LinearEasing))

        // 2. Status appears (300ms delay)
        delay(300)
        statusText = "[SYS] / ${STATUS_MESSAGES[0]}"
        statusIndex = 0

        // 3. ASCII art typewriter
        delay(300)
        for (i in 0 until totalChars) {
            displayedChars = i + 1
            delay(CHAR_DELAY_MS)
        }

        // 4. Progress bar fills
        statusIndex = 1
        statusText = "[SYS] / ${STATUS_MESSAGES[1]}"
        for (i in 1..PROGRESS_SEGMENTS) {
            progressFill = i
            if (i == PROGRESS_SEGMENTS / 2) {
                statusIndex = 2
                statusText = "[SYS] / ${STATUS_MESSAGES[2]}"
            }
            delay(PROGRESS_STEP_DELAY_MS)
        }

        // 5. READY — short pause then transition
        statusIndex = 3
        statusText = "[SYS] / READY"
        delay(400)
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(bgAlpha.value)
            .background(AgentShellColors.Shell0),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Status line
            Text(
                text = statusText,
                color = AgentShellColors.Text1,
                style = AgentShellTypography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ASCII art — char by char reveal
            Text(
                text = fullLogoText.take(displayedChars),
                color = AgentShellColors.Amber,
                style = AgentShellTypography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress bar
            val filled = "█".repeat(progressFill)
            val empty = "░".repeat(PROGRESS_SEGMENTS - progressFill)
            Text(
                text = "[$filled$empty] ${progressFill * 100 / PROGRESS_SEGMENTS}%",
                color = AgentShellColors.Text2,
                style = AgentShellTypography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = STATUS_MESSAGES.getOrElse(statusIndex) { "READY" },
                color = when (statusIndex) {
                    3    -> AgentShellColors.TermSys
                    else -> AgentShellColors.Text3
                },
                style = AgentShellTypography.labelSmall
            )
        }
    }
}
