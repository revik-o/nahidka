package org.orev.nahidka.feature.financial.dto

import kotlin.time.Instant
import kotlinx.datetime.YearMonth

data class ReportingPeriod(
    val month: YearMonth,
    val timeZoneIdentifier: String,
    val startInclusive: Instant,
    val endExclusive: Instant,
)
