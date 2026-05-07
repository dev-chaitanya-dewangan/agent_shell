package dev.agentshell.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import dev.agentshell.app.llm.ProviderType
import dev.agentshell.app.ui.components.ShellButton
import dev.agentshell.app.ui.components.ShellInput
import dev.agentshell.app.ui.components.ShellPanel
import dev.agentshell.app.ui.components.Spacing
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var csvInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgentShellColors.Shell0)
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Provider Selection
        ShellPanel(header = "AI ENGINE PROVIDER") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                ProviderTab(
                    label = "GOOGLE GEMINI",
                    isSelected = state.provider == ProviderType.GOOGLE_GEMINI,
                    onClick = { viewModel.setProvider(ProviderType.GOOGLE_GEMINI) },
                    modifier = Modifier.weight(1f)
                )
                ProviderTab(
                    label = "OPENROUTER",
                    isSelected = state.provider == ProviderType.OPENROUTER,
                    onClick = { viewModel.setProvider(ProviderType.OPENROUTER) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // API Key
        ShellPanel(header = "API CONFIGURATION") {
            Text(
                text = "Enter API Key for ${state.provider.name}:",
                color = AgentShellColors.Text1,
                style = AgentShellTypography.bodyLarge,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
            ShellInput(
                value = state.activeKey,
                onValueChange = viewModel::updateApiKey,
                placeholder = "sk-...",
                onSubmit = {},
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Model Selection
        ShellPanel(header = "MODEL SELECTION", modifier = Modifier.weight(1f)) {
            Text(
                text = "Active: ${state.selectedModel}",
                color = AgentShellColors.Amber,
                style = AgentShellTypography.bodyLarge,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                items(state.availableModels) { model ->
                    val isSelected = model == state.selectedModel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setModel(model) }
                            .background(if (isSelected) AgentShellColors.Shell2 else AgentShellColors.Shell1)
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSelected) "[*] " else "[ ] ",
                            color = if (isSelected) AgentShellColors.Amber else AgentShellColors.Text2,
                            style = AgentShellTypography.bodyLarge
                        )
                        Text(
                            text = model,
                            color = if (isSelected) AgentShellColors.Text0 else AgentShellColors.Text1,
                            style = AgentShellTypography.bodyLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = "Add Custom Models (Comma-separated):",
                color = AgentShellColors.Text2,
                style = AgentShellTypography.bodySmall,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ShellInput(
                        value = csvInput,
                        onValueChange = { csvInput = it },
                        placeholder = "gemini-1.5-pro, etc...",
                        onSubmit = {
                            if (csvInput.isNotBlank()) {
                                viewModel.addCustomModels(csvInput)
                                csvInput = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ShellButton(
                    label = "ADD",
                    onClick = {
                        if (csvInput.isNotBlank()) {
                            viewModel.addCustomModels(csvInput)
                            csvInput = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProviderTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) AgentShellColors.Shell2 else AgentShellColors.Shell1)
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) AgentShellColors.Amber else AgentShellColors.Text2,
            style = AgentShellTypography.bodyLarge
        )
    }
}
