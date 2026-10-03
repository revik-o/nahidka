package org.orev.nahidka.feature.financial.support

import dev.zacsweers.metro.Inject
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.Money

class ExactMoneyFormatter @Inject constructor() {
    fun format(money: Money, asset: AssetDefinition, decimalSeparator: Char = '.'): String {
        require(money.assetId == asset.id)
        val negative = money.units < 0
        val magnitude = if (negative) {
            (-(money.units + 1)).toULong() + 1uL
        } else {
            money.units.toULong()
        }
        val digits = magnitude.toString().padStart(asset.fractionDigits + 1, '0')
        val whole = if (asset.fractionDigits == 0) digits else digits.dropLast(asset.fractionDigits)
        val fraction = if (asset.fractionDigits == 0) "" else "$decimalSeparator${digits.takeLast(asset.fractionDigits)}"
        return "${if (negative) "-" else ""}$whole$fraction ${asset.displayCode}"
    }
}
