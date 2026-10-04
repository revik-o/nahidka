package org.orev.nahidka.feature.financial.calculation

import dev.zacsweers.metro.Inject
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import org.orev.nahidka.feature.financial.dto.ReportingPeriod

class FinancialCalendar @Inject constructor() {

    fun reportingPeriod(month: YearMonth, timeZoneIdentifier: String): ReportingPeriod {
        val timeZone = TimeZone.of(timeZoneIdentifier)
        val monthNumber = month.month.ordinal + 1

        val nextMonth = if (monthNumber == 12) {
            YearMonth(month.year + 1, 1)
        } else {
            YearMonth(month.year, monthNumber + 1)
        }

        return ReportingPeriod(
            month = month,
            timeZoneIdentifier = timeZoneIdentifier,
            startInclusive = LocalDate(month.year, monthNumber, 1).atStartOfDayIn(timeZone),
            endExclusive = LocalDate(nextMonth.year, nextMonth.month.ordinal + 1, 1).atStartOfDayIn(timeZone),
        )
    }
}
