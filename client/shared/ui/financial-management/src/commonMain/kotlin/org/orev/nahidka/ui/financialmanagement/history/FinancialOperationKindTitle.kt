package org.orev.nahidka.ui.financialmanagement.history

import kotlinx.collections.immutable.persistentListOf
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_kind_expense
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_kind_income
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_operation_kind_refund
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.feature.financial.dto.OperationKind

internal val FINANCIAL_OPERATION_CREATION_KINDS = persistentListOf(OperationKind.EXPENSE, OperationKind.INCOME)

internal val OperationKind.title: StringResource
    get() = when (this) {
        OperationKind.EXPENSE -> Res.string.financialmanagement_operation_kind_expense
        OperationKind.INCOME -> Res.string.financialmanagement_operation_kind_income
        OperationKind.REFUND -> Res.string.financialmanagement_operation_kind_refund
    }
