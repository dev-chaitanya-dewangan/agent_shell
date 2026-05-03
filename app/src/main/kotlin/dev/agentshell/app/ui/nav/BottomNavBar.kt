package dev.agentshell.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.agentshell.app.ui.components.Borders
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

enum class NavRoute(val title: String) {
    SHELL("SHELL"),
    CHAT("CHAT"),
    APPS("APPS"),
    SETTINGS("SETTINGS")
}

@Composable
fun BottomNavBar(
    currentRoute: NavRoute,
    onNavigate: (NavRoute) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AgentShellColors.Shell1)
            .border(width = Borders.accent, color = AgentShellColors.Shell3),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavRoute.values().forEach { route ->
            val isSelected = currentRoute == route
            val color = if (isSelected) AgentShellColors.Amber else AgentShellColors.Text3
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Placeholder for icons. 
                Text(
                    text = if (isSelected) "[${route.title}]" else route.title,
                    color = color,
                    style = AgentShellTypography.labelSmall
                )
            }
        }
    }
}
