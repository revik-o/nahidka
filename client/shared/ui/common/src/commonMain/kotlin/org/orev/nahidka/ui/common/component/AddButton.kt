package org.orev.nahidka.ui.common.component

import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.orev.nahidka.ui.common.layout.LayoutWidth

@Composable
fun AddButton(
    title: String,
    layoutWidth: LayoutWidth,
    onClick: () -> Unit,
) {
    when (layoutWidth) {
        LayoutWidth.COMPACT -> FilledIconButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = title },
        ) {
            Text("＋")
        }

        LayoutWidth.EXPANDED -> Button(onClick = onClick) {
            Text(title)
        }
    }
}
