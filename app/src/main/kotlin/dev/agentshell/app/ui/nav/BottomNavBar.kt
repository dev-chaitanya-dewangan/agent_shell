package dev.agentshell.app.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
    onNavigate: (NavRoute) -> Unit,
    onVoiceClick: () -> Unit = {},
    isVoiceActive: Boolean = false
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
        val leftRoutes = listOf(NavRoute.SHELL, NavRoute.CHAT)
        val rightRoutes = listOf(NavRoute.APPS, NavRoute.SETTINGS)

        leftRoutes.forEach { route ->
            NavButton(route, currentRoute == route, onNavigate)
        }

        // Voice Button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isVoiceActive) AgentShellColors.Amber else AgentShellColors.Shell2)
                    .clickable { onVoiceClick() },
                contentAlignment = Alignment.Center
            ) {
                // If you have a mic icon you can use Icon()
                // using text for now to be safe with missing resources
                Text(
                    text = "MIC",
                    color = if (isVoiceActive) AgentShellColors.Shell0 else AgentShellColors.Amber,
                    style = AgentShellTypography.labelSmall
                )
            }
        }

        rightRoutes.forEach { route ->
            NavButton(route, currentRoute == route, onNavigate)
        }
    }
}

@Composable
private fun RowScope.NavButton(
    route: NavRoute,
    isSelected: Boolean,
    onNavigate: (NavRoute) -> Unit
) {
    val color = if (isSelected) AgentShellColors.Amber else AgentShellColors.Text3
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onNavigate(route) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSelected) "[${route.title}]" else route.title,
            color = color,
            style = AgentShellTypography.labelSmall
        )
    }
}
