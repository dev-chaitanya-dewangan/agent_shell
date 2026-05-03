package dev.agentshell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

object Borders {
    val thin = 0.5.dp
    val standard = 1.dp
    val accent = 2.dp
    val thick = 3.dp
}

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun ShellPanel(
    header: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .border(Borders.standard, AgentShellColors.Shell3)
            .background(AgentShellColors.Shell1)
    ) {
        header?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = Spacing.sm)
                    .border(Borders.thin, AgentShellColors.Shell3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = it,
                    style = AgentShellTypography.bodySmall, // Used instead of body12
                    color = AgentShellColors.Amber
                )
            }
        }
        Column(modifier = Modifier.padding(Spacing.sm), content = content)
    }
}
