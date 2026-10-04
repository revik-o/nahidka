package org.orev.nahidka.ui.financialmanagement.component

import nahidka.shared.ui.financial_management.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.feature.financial.dto.PaymentMethod

internal val PaymentMethod.title: StringResource
    get() = when (this) {
        PaymentMethod.CARD -> Res.string.financialmanagement_payment_method_card
        PaymentMethod.CRYPTOCURRENCY -> Res.string.financialmanagement_payment_method_cryptocurrency
        PaymentMethod.CASH -> Res.string.financialmanagement_payment_method_cash
    }
