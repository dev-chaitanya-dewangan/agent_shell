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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agentshell.app.chat.ChatScreen
import dev.agentshell.app.terminal.TerminalScreen
import dev.agentshell.app.miniapp.MiniAppsScreen
import dev.agentshell.app.miniapp.MiniAppDetailScreen
import dev.agentshell.app.miniapp.MiniAppsViewModel
import dev.agentshell.app.ui.apps.AppsHubScreen
import dev.agentshell.app.ui.splash.SplashScreen
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

/**
 * Root navigation host.
 *
 * Shows [SplashScreen] on first launch, then transitions to the main
 * bottom-nav scaffold via a Crossfade.  All four tabs live inside the Scaffold.
 *
 * APPS tab now shows [AppsHubScreen] first (connected apps + mini-apps card).
 * Tapping the mini-apps card navigates into [MiniAppsFlow].
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
    val voiceViewModel: dev.agentshell.app.voice.VoiceAgentViewModel = hiltViewModel()
    val isListening by voiceViewModel.isListening.collectAsState()
    val spokenText by voiceViewModel.spokenText.collectAsState()
    val agentState by voiceViewModel.agentState.collectAsState()

    Scaffold(
        containerColor = AgentShellColors.Shell0,
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate,
                onVoiceClick = { voiceViewModel.startListening() },
                isVoiceActive = isListening
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
                NavRoute.APPS     -> AppsFlow()
                NavRoute.SETTINGS -> dev.agentshell.app.ui.settings.SettingsScreen()
            }

            // Voice Overlay floating right above the bottom bar
            dev.agentshell.app.ui.components.VoiceOverlayUI(
                agentState = agentState,
                spokenText = spokenText,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * APPS tab flow:
 *   AppsHubScreen (connected apps + mini-apps entry card)
 *     → MiniAppsScreen (list of agent-generated mini-apps)
 *       → MiniAppDetailScreen (WebView for selected mini-app)
 */
@Composable
fun AppsFlow() {
    // null    = show AppsHub
    // ""      = show MiniApps list
    // <id>    = show specific mini-app detail
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    var inMiniApps    by remember { mutableStateOf(false) }

    val miniAppsViewModel: MiniAppsViewModel = hiltViewModel()
    val miniApps by miniAppsViewModel.miniApps.collectAsState()

    when {
        selectedAppId != null -> {
            MiniAppDetailScreen(appId = selectedAppId!!, onBack = { selectedAppId = null })
        }
        inMiniApps -> {
            // Provide empty lambda or modify MiniAppsScreen to take onBack
            MiniAppsScreen(onAppClick = { selectedAppId = it })
        }
        else -> {
            AppsHubScreen(
                miniApps = miniApps,
                onOpenMiniApp = { selectedAppId = it },
                onOpenMiniAppsList = { inMiniApps = true }
            )
        }
    }
}

// Legacy alias kept for safety — unused but avoids any stale references
@Composable
fun MiniAppsFlow() {
    var selectedAppId by remember { mutableStateOf<String?>(null) }
    if (selectedAppId == null) {
        MiniAppsScreen(onAppClick = { selectedAppId = it })
    } else {
        MiniAppDetailScreen(appId = selectedAppId!!, onBack = { selectedAppId = null })
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
