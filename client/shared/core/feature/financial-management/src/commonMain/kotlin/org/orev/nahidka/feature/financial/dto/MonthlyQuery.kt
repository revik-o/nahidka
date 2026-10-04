package org.orev.nahidka.feature.financial.dto

import kotlinx.datetime.YearMonth

data class MonthlyQuery(
    val month: YearMonth,
    val assetIdentifier: String,
)
