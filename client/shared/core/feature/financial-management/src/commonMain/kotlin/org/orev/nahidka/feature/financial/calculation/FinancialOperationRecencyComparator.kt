package org.orev.nahidka.feature.financial.calculation

import org.orev.nahidka.feature.financial.dto.FinancialOperation

object FinancialOperationRecencyComparator : Comparator<FinancialOperation> {

    override fun compare(first: FinancialOperation, second: FinancialOperation): Int {
        val occurrenceOrder = second.occurredAt.compareTo(first.occurredAt)

        return if (occurrenceOrder != 0) {
            occurrenceOrder
        } else {
            first.identifier.compareTo(second.identifier)
        }
    }
}
