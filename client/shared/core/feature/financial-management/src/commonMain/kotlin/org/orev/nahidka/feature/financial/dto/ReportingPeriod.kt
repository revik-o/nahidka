package org.orev.nahidka.feature.financial.dto

import kotlin.time.Instant
import kotlinx.datetime.YearMonth

data class ReportingPeriod(
    val month: YearMonth,
    val timeZoneId: String,
    val startInclusive: Instant,
    val endExclusive: Instant,
)
