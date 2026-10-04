package org.orev.nahidka.ui.financialmanagement.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.ui.financialmanagement.common.parsePositiveMoney

@Composable
internal fun FinancialMoneyField(
    amountText: String,
    asset: AssetDefinition,
    title: String,
    onAmountTextChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = amountText,
        onValueChange = onAmountTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(title) },
        suffix = { Text(asset.displayCode) },
        isError = amountText.isNotBlank() && parsePositiveMoney(amountText, asset) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
    )
}
