package org.orev.nahidka.feature.financial.support

import dev.zacsweers.metro.Inject
import org.orev.nahidka.feature.financial.calculation.formatMoneyInput
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.Money

class ExactMoneyFormatter @Inject constructor() {

    fun format(money: Money, asset: AssetDefinition, decimalSeparator: Char = '.'): String =
        "${formatMoneyInput(money, asset, decimalSeparator)} ${asset.displayCode}"
}
