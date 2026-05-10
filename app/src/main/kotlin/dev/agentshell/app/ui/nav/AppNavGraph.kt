package dev.agentshell.app.ui.nav

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import dev.agentshell.app.chat.ChatScreen
import dev.agentshell.app.terminal.TerminalScreen
import dev.agentshell.app.miniapp.MiniAppsScreen
import dev.agentshell.app.miniapp.MiniAppDetailScreen
import dev.agentshell.app.ui.splash.SplashScreen
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

/**
 * Root navigation host.
 *
 * Shows [SplashScreen] on first launch, then transitions to the main
 * bottom-nav scaffold via a Crossfade.  All four tabs (SHELL, CHAT,
 * APPS, SETTINGS) live inside the Scaffold.
 */
@Composable
fun AppNavGraph() {
    var splashComplete by remember { mutableStateOf(false) }
    var currentRoute by remember { mutableStateOf(NavRoute.SHELL) }

    Crossfade(
        targetState = splashComplete,
        animationSpec = tween(400),
        label = "splash_crossfade"
    ) { ready ->
        if (!ready) {
            SplashScreen(onAnimationComplete = { splashComplete = true })
        } else {
            MainScaffold(
                currentRoute = currentRoute,
                onNavigate = { currentRoute = it }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    currentRoute: NavRoute,
    onNavigate: (NavRoute) -> Unit
) {
    Scaffold(
        containerColor = AgentShellColors.Shell0,
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRoute) {
                NavRoute.SHELL    -> TerminalScreen()
                NavRoute.CHAT     -> ChatScreen()
                NavRoute.APPS     -> MiniAppsFlow()
                NavRoute.SETTINGS -> dev.agentshell.app.ui.settings.SettingsScreen()
            }
        }
    }
}

@Composable
fun MiniAppsFlow() {
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    if (selectedAppId == null) {
        MiniAppsScreen(onAppClick = { selectedAppId = it })
    } else {
        MiniAppDetailScreen(appId = selectedAppId!!)
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
            Text(
                text = "// COMING SOON",
                color = AgentShellColors.Text3,
                style = AgentShellTypography.labelSmall
            )
        }
    }
}
