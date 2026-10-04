package org.orev.nahidka.feature.financial.dto

data class PlanningNotConfigured(
    val period: ReportingPeriod,
    val income: Money,
    val grossExpenses: Money,
    val refunds: Money,
    val netExpense: Money,
    val currentAvailable: Money,
) : PlanningState
