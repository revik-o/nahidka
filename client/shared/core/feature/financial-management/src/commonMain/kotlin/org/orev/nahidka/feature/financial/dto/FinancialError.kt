package org.orev.nahidka.feature.financial.dto

sealed interface FinancialError {

    data class Validation(val field: String, val message: String) : FinancialError

    data class NotFound(val entity: String, val identifier: String) : FinancialError

    data class OperationConflict(
        val expectedVersion: Long,
        val current: FinancialOperation,
    ) : FinancialError

    data class PlanningConflict(
        val expectedVersion: Long?,
        val current: FinancialPlanningTable,
    ) : FinancialError

    data class CategoryConflict(
        val expectedVersion: Long,
        val current: FinancialCategory,
    ) : FinancialError

    data class CategoryInUse(val identifier: String) : FinancialError

    data class CommandIdentifierReused(val commandIdentifier: String) : FinancialError

    data object SessionClosed : FinancialError

    data object StorageUnavailable : FinancialError

    data object Forbidden : FinancialError
}
