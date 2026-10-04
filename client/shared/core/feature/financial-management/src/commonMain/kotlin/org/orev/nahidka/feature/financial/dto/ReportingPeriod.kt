package org.orev.nahidka.feature.financial.dto

import kotlinx.datetime.YearMonth
import kotlin.time.Instant

data class ReportingPeriod(
    val month: YearMonth,
    val timeZoneIdentifier: String,
    val startInclusive: Instant,
    val endExclusive: Instant,
)
