package org.orev.nahidka.feature.financial

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class FinancialService {
    fun addTransaction(amount: Double, description: String): Deferred<Unit> {
        return CompletableDeferred<Unit>().apply { complete(Unit) }
    }
}
