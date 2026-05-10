package dev.agentshell.app.miniapp

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.components.ShellPanel
import java.io.File

@Composable
fun MiniAppDetailScreen(
    appId: String,
    viewModel: MiniAppsViewModel = hiltViewModel()
) {
    val miniApp = remember { mutableStateOf<MiniAppEntity?>(null) }
    val dao = (viewModel as? MiniAppsViewModel)?.javaClass?.getDeclaredField("dao")?.apply { isAccessible = true }?.get(viewModel) as? MiniAppDao

    LaunchedEffect(appId) {
        dao?.getById(appId)?.let { app ->
            miniApp.value = app
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgentShellColors.TermBg)
    ) {
        ShellPanel(
            header = "MINI APP // ${miniApp.value?.name ?: "LOADING"}",
            modifier = Modifier.fillMaxSize()
        ) {
            val app = miniApp.value
            if (app != null) {
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
                }
            }
        }
    }
}
