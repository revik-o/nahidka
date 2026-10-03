package org.orev.nahidka.feature.financial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod

class FinancialCalendarTest {
    @Test
    fun reportingPeriodsUseIanaBoundariesAcrossLeapDayAndDaylightSaving() {
        val calendar = FinancialCalendar()
        val leapFebruary = calendar.reportingPeriod(YearMonth(2024, 2), "Europe/Kyiv")
        assertEquals(696, (leapFebruary.endExclusive - leapFebruary.startInclusive).inWholeHours)

        val spring = calendar.reportingPeriod(YearMonth(2024, 3), "Europe/Kyiv")
        assertEquals(743, (spring.endExclusive - spring.startInclusive).inWholeHours)
        assertEquals(YearMonth(2024, 3), financialMonthFor(operationAt(Instant.parse("2024-03-31T20:59:59Z")), "Europe/Kyiv"))
        assertEquals(YearMonth(2024, 4), financialMonthFor(operationAt(Instant.parse("2024-03-31T21:00:00Z")), "Europe/Kyiv"))

        val autumn = calendar.reportingPeriod(YearMonth(2024, 10), "Europe/Kyiv")
        assertEquals(745, (autumn.endExclusive - autumn.startInclusive).inWholeHours)
    }

    private fun operationAt(instant: Instant) = FinancialOperation(
        id = "boundary",
        version = 1,
        amount = Money("iso4217:USD", 1),
        kind = OperationKind.INCOME,
        categoryId = null,
        paymentMethod = PaymentMethod.CASH,
        occurredAt = instant,
        description = null,
        refundOfOperationId = null,
    )
}
