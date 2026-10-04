package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.Money

fun parseMoneyText(
    text: String,
    asset: AssetDefinition,
    decimalSeparator: Char = '.',
    groupingSeparator: Char? = if (decimalSeparator == '.') ',' else '.',
): Money? {
    val value = text.trim()

    if (value.isEmpty() || decimalSeparator == groupingSeparator) {
        return null
    }

    val negative = value.startsWith('-')
    val unsigned = if (negative || value.startsWith('+')) value.drop(1) else value

    if (unsigned.isEmpty()) {
        return null
    }

    val parts = unsigned.split(decimalSeparator)

    if (parts.size > 2 || parts.any { it.isEmpty() && parts.size == 1 }) {
        return null
    }

    val wholePart = parts[0]
    val fractionPart = parts.getOrElse(1) { "" }

    if (fractionPart.any { it !in '0'..'9' } || fractionPart.length > asset.fractionDigits) {
        return null
    }

    val wholeDigits = if (groupingSeparator != null && groupingSeparator in wholePart) {
        val groups = wholePart.split(groupingSeparator)

        if (groups.isEmpty() || groups.first().length !in 1..3 || groups.drop(1).any { it.length != 3 }) {
            return null
        }

        groups.joinToString("")
    } else {
        wholePart
    }

    if (wholeDigits.isEmpty() || wholeDigits.any { it !in '0'..'9' }) {
        return null
    }

    var magnitude = 0uL
    val digits = wholeDigits + fractionPart.padEnd(asset.fractionDigits, '0')

    for (digitChar in digits) {
        val digit = (digitChar - '0').toULong()

        if (magnitude > (ULong.MAX_VALUE - digit) / 10uL) {
            return null
        }

        magnitude = magnitude * 10uL + digit
    }

    val maximum = if (negative) {
        Long.MAX_VALUE.toULong() + 1uL
    } else {
        Long.MAX_VALUE.toULong()
    }

    if (magnitude > maximum) {
        return null
    }

    val units = when {
        negative && magnitude == Long.MAX_VALUE.toULong() + 1uL -> Long.MIN_VALUE
        negative -> -magnitude.toLong()
        else -> magnitude.toLong()
    }

    return Money(asset.identifier, units)
}

fun formatMoneyInput(money: Money, asset: AssetDefinition, decimalSeparator: Char = '.'): String {
    require(money.assetIdentifier == asset.identifier)
    val negative = money.units < 0

    val magnitude = if (negative) {
        (-(money.units + 1)).toULong() + 1uL
    } else {
        money.units.toULong()
    }

    val digits = magnitude.toString().padStart(asset.fractionDigits + 1, '0')
    val whole = if (asset.fractionDigits == 0) digits else digits.dropLast(asset.fractionDigits)
    val fraction = if (asset.fractionDigits == 0) "" else "$decimalSeparator${digits.takeLast(asset.fractionDigits)}"

    return "${if (negative) "-" else ""}$whole$fraction"
}
