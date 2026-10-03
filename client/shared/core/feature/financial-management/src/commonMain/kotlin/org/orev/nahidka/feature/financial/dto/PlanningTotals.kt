package org.orev.nahidka.feature.financial.dto

data class PlanningTotals(
    val fundsAvailableThisMonth: Money,
    val grossExpenses: Money,
    val refunds: Money,
    val currentAvailable: Money,
    val includedBudget: Money,
    val remainingReservation: Money,
    val projectedAvailableAfterPlanning: Money,
    val unplannedNetExpense: Money,
    val suggestedSavings: Money?,
)
