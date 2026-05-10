package dev.agentshell.app.miniapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    val buildingAppName by viewModel.buildingAppName.collectAsState()

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
            if (miniApps.isEmpty() && buildingAppName == null) {
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
                    // Show "building" placeholder at the top if LLM is generating one
                    if (buildingAppName != null) {
                        item {
                            BuildingAppRow(name = buildingAppName!!)
                        }
                    }

                    items(miniApps) { app ->
                        AppItemRow(
                            name = app.name,
                            description = app.description,
                            onClick = { onAppClick(app.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildingAppRow(name: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "building")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppAvatar(name = name)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "$name [BUILDING...]",
                color = AgentShellColors.Amber,
                style = AgentShellTypography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "LLM is writing HTML/JS/CSS...",
                color = AgentShellColors.Text3,
                style = AgentShellTypography.bodyMedium
            )
        }
    }
}

@Composable
private fun AppItemRow(
    name: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppAvatar(name = name)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = name,
                color = AgentShellColors.TermCmd,
                style = AgentShellTypography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = AgentShellColors.Text2,
                style = AgentShellTypography.bodyMedium
            )
        }
    }
}

@Composable
private fun AppAvatar(name: String) {
    val initials = name
        .split(" ")
        .mapNotNull { it.firstOrNull() }
        .take(2)
        .joinToString("")
        .uppercase()

    val display = if (initials.isEmpty()) "??" else initials

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AgentShellColors.Shell2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = display,
            color = AgentShellColors.Text1,
            style = AgentShellTypography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
