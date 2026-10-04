package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val FINANCIAL_TABLE_FOOTER_KEY = "financial-table-footer"

@Composable
internal fun <Row> FinancialTable(
    rows: List<Row>,
    rowKey: (Row) -> Any,
    emptyTableMessage: String,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    rowContent: @Composable (Row) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium),
    ) {
        if (rows.isEmpty()) {
            Text(
                text = emptyTableMessage,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(rows, key = rowKey) { row ->
                    rowContent(row)
                    HorizontalDivider()
                }
                footer?.let { tableFooter ->
                    item(key = FINANCIAL_TABLE_FOOTER_KEY) { tableFooter() }
                }
            }
        }
    }
}
