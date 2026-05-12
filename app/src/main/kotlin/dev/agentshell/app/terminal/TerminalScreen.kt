package dev.agentshell.app.terminal

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val context = LocalContext.current

    // Auto-scroll to bottom when new log arrives
    LaunchedEffect(state.outputLog.size) {
        if (state.outputLog.isNotEmpty()) {
            listState.animateScrollToItem(state.outputLog.size - 1)
        }
    }

    // Re-check permission every time the screen is composed (user may have just granted it)
    var permGranted by remember { mutableStateOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else true
    )}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AgentShellColors.TermBg)
            .imePadding()
    ) {
        // ── Storage Permission Banner ────────────────────────────────────────
        if (state.needsStoragePermission || !permGranted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AgentShellColors.Error.copy(alpha = 0.15f))
                    .border(1.dp, AgentShellColors.Error)
                    .clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "⚠ Storage Permission Needed",
                        color = AgentShellColors.Error,
                        style = AgentShellTypography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap here → grant 'All Files Access' → come back",
                        color = AgentShellColors.Text3,
                        style = AgentShellTypography.labelSmall
                    )
                }
                Text(
                    text = "GRANT →",
                    color = AgentShellColors.Error,
                    style = AgentShellTypography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

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
                        logLine.startsWith(">")      -> AgentShellColors.TermCmd
                        logLine.startsWith("[Error") -> AgentShellColors.TermErr
                        logLine.startsWith("[PERM]") -> AgentShellColors.Error
                        logLine.startsWith("[OK]")   -> AgentShellColors.Success
                        logLine.startsWith("[FIX]")  -> AgentShellColors.Amber
                        logLine.startsWith("[INFO]") -> AgentShellColors.Text2
                        else                         -> AgentShellColors.TermOut
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
