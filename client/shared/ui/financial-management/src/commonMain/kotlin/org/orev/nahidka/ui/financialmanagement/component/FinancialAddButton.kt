package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
internal fun FinancialAddButton(
    title: String,
    financialLayoutWidth: FinancialLayoutWidth,
    onClick: () -> Unit,
) {
    when (financialLayoutWidth) {
        FinancialLayoutWidth.COMPACT -> FilledIconButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = title },
        ) {
            Text("＋")
        }

        FinancialLayoutWidth.EXPANDED -> Button(onClick = onClick) {
            Text(title)
        }
    }
}
