package dev.agentshell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.agentshell.app.ui.theme.AgentShellTheme
import dev.agentshell.app.ui.theme.AgentShellColors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentShellTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AgentShellColors.Shell0),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "[SYS] / INIT Â·Â·Â·",
                        color = AgentShellColors.Amber
                    )
                }
            }
        }
    }
}
