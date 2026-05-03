package dev.agentshell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

enum class AgentTier {
    LOCAL, SELFHOST, API
}

@Composable
fun TierSelectorBar(
    selected: AgentTier,
    onSelect: (AgentTier) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .border(Borders.standard, AgentShellColors.Shell3)
    ) {
        AgentTier.values().forEach { tier ->
            val isSelected = selected == tier
            val backgroundColor = if (isSelected) AgentShellColors.Amber else AgentShellColors.Shell1
            val textColor = if (isSelected) AgentShellColors.Shell0 else AgentShellColors.Text2
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(backgroundColor)
                    .clickable { onSelect(tier) }
                    .border(Borders.thin, AgentShellColors.Shell3),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${tier.name} ${if (isSelected) "●" else "○"}",
                    color = textColor,
                    style = AgentShellTypography.labelSmall
                )
            }
        }
    }
}
