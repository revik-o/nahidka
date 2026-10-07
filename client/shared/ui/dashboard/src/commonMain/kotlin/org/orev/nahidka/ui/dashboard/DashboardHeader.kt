package org.orev.nahidka.ui.dashboard

import kotlinx.datetime.LocalDate

data class DashboardHeader(
    val dayPeriod: DayPeriod,
    val today: LocalDate,
)
