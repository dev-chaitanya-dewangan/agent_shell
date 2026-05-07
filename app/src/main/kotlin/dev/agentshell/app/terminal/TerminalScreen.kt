package dev.agentshell.app.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.agentshell.app.ui.components.ShellInput
import dev.agentshell.app.ui.components.ShellPanel
import dev.agentshell.app.ui.components.Spacing
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new log arrives
    LaunchedEffect(state.outputLog.size) {
        if (state.outputLog.isNotEmpty()) {
            listState.animateScrollToItem(state.outputLog.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgentShellColors.TermBg)
    ) {
        ShellPanel(
            header = "TERMINAL // LOCAL",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(Spacing.sm)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.outputLog) { logLine ->
                    val textColor = when {
                        logLine.startsWith(">") -> AgentShellColors.TermCmd
                        logLine.startsWith("[Error") -> AgentShellColors.TermErr
                        else -> AgentShellColors.TermOut
                    }
                    Text(
                        text = logLine,
                        color = textColor,
                        style = AgentShellTypography.bodyLarge
                    )
                }
            }
        }

        ShellInput(
            value = state.currentInput,
            onValueChange = viewModel::onInputChanged,
            placeholder = if (state.isExecuting) "Executing..." else "Type a command...",
            onSubmit = viewModel::submitCommand,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
