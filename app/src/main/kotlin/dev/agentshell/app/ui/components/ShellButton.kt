package dev.agentshell.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellShapes
import dev.agentshell.app.ui.theme.AgentShellTypography

enum class ButtonStyle {
    PRIMARY, SECONDARY, DANGER
}

@Composable
fun ShellButton(
    label: String,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor = when (style) {
        ButtonStyle.PRIMARY -> AgentShellColors.Amber
        ButtonStyle.SECONDARY -> AgentShellColors.Shell3
        ButtonStyle.DANGER -> AgentShellColors.Error
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPressed) {
        AgentShellColors.Shell1
    } else {
        Color.Transparent
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = AgentShellShapes.small,
        border = BorderStroke(Borders.standard, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = AgentShellColors.Text0,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AgentShellColors.Text3
        ),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm)
    ) {
        Text(
            text = label.uppercase(),
            style = AgentShellTypography.bodySmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}
