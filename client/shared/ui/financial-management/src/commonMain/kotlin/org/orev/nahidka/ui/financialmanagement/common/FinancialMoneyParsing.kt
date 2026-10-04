package org.orev.nahidka.ui.financialmanagement.common

import org.orev.nahidka.feature.financial.calculation.parseMoneyText
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.Money

internal fun parsePositiveMoney(amountText: String, asset: AssetDefinition): Money? =
    parseMoneyText(amountText, asset)?.takeIf { money -> money.units > 0 }
