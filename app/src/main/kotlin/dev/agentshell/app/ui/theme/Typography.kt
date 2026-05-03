package dev.agentshell.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import dev.agentshell.app.R // Make sure you import your R file properly if you add custom fonts

// For now, we will use the default monospaced font until a JetBrains Mono font file is added.
val JetBrainsMono = FontFamily.Monospace

val AgentShellTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 24.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 11.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontSize = 9.sp
    )
)
