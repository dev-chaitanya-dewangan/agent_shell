package dev.agentshell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import dev.agentshell.app.ui.theme.AgentShellColors
import dev.agentshell.app.ui.theme.AgentShellTypography

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@Composable
fun ShellInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    onSubmit: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AgentShellColors.Shell2)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "> ",
            color = AgentShellColors.Amber,
            style = AgentShellTypography.bodyLarge
        )
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = AgentShellTypography.bodyLarge.copy(color = AgentShellColors.TermCmd),
            cursorBrush = SolidColor(AgentShellColors.TermCur),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = AgentShellColors.Text3,
                        style = AgentShellTypography.bodyLarge
                    )
                }
                innerTextField()
            }
        )
    }
}
