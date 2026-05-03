package dev.agentshell.app.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

@Composable
fun AppNavGraph() {
    var currentRoute by remember { mutableStateOf(NavRoute.SHELL) }

    Scaffold(
        containerColor = AgentShellColors.Shell0,
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route -> currentRoute = route }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                NavRoute.SHELL -> PlaceholderScreen("Terminal Environment")
                NavRoute.CHAT -> PlaceholderScreen("Agent Chat Interface")
                NavRoute.APPS -> PlaceholderScreen("Mini Apps")
                NavRoute.SETTINGS -> PlaceholderScreen("Settings")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "[SYS] / $title",
                color = AgentShellColors.Amber,
                style = AgentShellTypography.bodyLarge
            )
        }
    }
}
