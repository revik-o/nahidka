package org.orev.nahidka.ui.financialmanagement.common

import org.orev.nahidka.feature.financial.dto.FinancialError

data class FinancialDialogState<Draft>(
    val draft: Draft,
    val submittable: Boolean,
    val submitting: Boolean = false,
    val rejection: FinancialError? = null,
)
