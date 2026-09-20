package org.orev.nahidka.ui.common.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NahidkaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.medium,
        content = content
    )
}

@Preview
@Composable
fun NahidkaButtonPreview() {
    NahidkaTheme {
        NahidkaButton(onClick = {}) {
            Text("Button")
        }
    }
}
