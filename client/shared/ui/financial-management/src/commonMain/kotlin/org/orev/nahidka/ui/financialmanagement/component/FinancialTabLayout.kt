package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun FinancialTabLayout(
    financialLayoutWidth: FinancialLayoutWidth,
    tabSelector: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (financialLayoutWidth == FinancialLayoutWidth.COMPACT) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (financialLayoutWidth) {
            FinancialLayoutWidth.COMPACT -> {
                tabSelector()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }

            FinancialLayoutWidth.EXPANDED -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabSelector()
                Spacer(Modifier.weight(1f))
                actions()
            }
        }
        content()
    }
}
