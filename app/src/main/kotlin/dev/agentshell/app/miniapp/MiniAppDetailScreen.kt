package dev.agentshell.app.miniapp

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography
import dev.agentshell.app.ui.components.ShellPanel
import java.io.File

@Composable
fun MiniAppDetailScreen(
    appId: String,
    viewModel: MiniAppsViewModel = hiltViewModel()
) {
    val miniApp = remember { mutableStateOf<MiniAppEntity?>(null) }

    LaunchedEffect(appId) {
        miniApp.value = viewModel.getById(appId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgentShellColors.TermBg)
    ) {
        ShellPanel(
            header = "MINI APP // ${miniApp.value?.name ?: "LOADING..."}",
            modifier = Modifier.fillMaxSize()
        ) {
            val app = miniApp.value
            if (app == null) {
                // Still loading — show nothing or a spinner
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Loading...",
                        color = AgentShellColors.Text3,
                        style = AgentShellTypography.bodyLarge
                    )
                }
            } else {
                val file = File(app.entryHtmlPath)
                if (file.exists()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.allowFileAccess = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                loadUrl("file://${file.absolutePath}")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // HTML file missing — show error with path
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "[ERROR] HTML file not found",
                                color = AgentShellColors.Error,
                                style = AgentShellTypography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = app.entryHtmlPath,
                                color = AgentShellColors.Text3,
                                style = AgentShellTypography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

