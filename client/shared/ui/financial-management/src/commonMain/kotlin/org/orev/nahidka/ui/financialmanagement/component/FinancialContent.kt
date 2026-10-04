package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_content_unavailable
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.financialmanagement.common.FinancialContentState

@Composable
internal fun <Content> FinancialContent(
    financialContentState: FinancialContentState<Content>,
    content: @Composable (Content) -> Unit,
) {
    when (financialContentState) {
        FinancialContentState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        FinancialContentState.Unavailable -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.financialmanagement_content_unavailable))
        }

        is FinancialContentState.Available -> content(financialContentState.content)
    }
}
