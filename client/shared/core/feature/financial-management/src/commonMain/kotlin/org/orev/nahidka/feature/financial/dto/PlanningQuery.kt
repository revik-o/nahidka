package org.orev.nahidka.feature.financial.dto

import kotlinx.datetime.YearMonth

data class PlanningQuery(
    val month: YearMonth? = null,
    val assetIdentifier: String? = null,
)
