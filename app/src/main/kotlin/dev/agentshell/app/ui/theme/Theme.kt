package dev.agentshell.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = AgentShellColors.Shell0,
    surface = AgentShellColors.Shell1,
    primary = AgentShellColors.Amber,
    onPrimary = AgentShellColors.Shell0,
    onBackground = AgentShellColors.Text1,
    onSurface = AgentShellColors.Text1,
    error = AgentShellColors.Error
)

@Composable
fun AgentShellTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = AgentShellShapes,
        typography = AgentShellTypography,
        content = content
    )
}
