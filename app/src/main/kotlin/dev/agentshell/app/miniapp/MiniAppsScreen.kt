package dev.agentshell.app.miniapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography
import dev.agentshell.app.ui.components.ShellPanel

@Composable
fun MiniAppsScreen(
    viewModel: MiniAppsViewModel = hiltViewModel(),
    onAppClick: (String) -> Unit
) {
    val miniApps by viewModel.miniApps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgentShellColors.TermBg)
            .padding(16.dp)
    ) {
        ShellPanel(
            header = "MINI APPS // GENERATED",
            modifier = Modifier.fillMaxSize()
        ) {
            if (miniApps.isEmpty()) {
                Text(
                    text = "No mini-apps generated yet.",
                    color = AgentShellColors.Text3,
                    style = AgentShellTypography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(miniApps) { app ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppClick(app.id) }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = app.name,
                                color = AgentShellColors.TermCmd,
                                style = AgentShellTypography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.description,
                                color = AgentShellColors.Text2,
                                style = AgentShellTypography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
