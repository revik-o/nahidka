package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.FinancialCategory

internal object FinancialCategoryNameComparator : Comparator<FinancialCategory> {

    override fun compare(first: FinancialCategory, second: FinancialCategory): Int {
        val nameOrder = first.name.compareTo(second.name, ignoreCase = true)

        return if (nameOrder != 0) {
            nameOrder
        } else {
            first.identifier.compareTo(second.identifier)
        }
    }
}
