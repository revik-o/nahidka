package org.orev.nahidka.feature.financial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FinancialStoreConcurrencyTest {
    @Test
    fun simultaneousCommandsOnMultipleDispatchersProduceOneOrderedLedger() = runTest {
        val fixture = FinancialTestFixture(journalCapacity = 128)
        val category = fixture.category()
        val results = (0 until 64).map { index ->
            async(Dispatchers.Default) {
                fixture.operation("parallel-$index", category.id, 100L + index, commandId = "parallel-command-$index")
            }
        }.awaitAll()
        assertEquals(64, results.map { it.id }.toSet().size)
        val snapshot = fixture.gateway.observeFinancialSnapshot(fixture.query()).first()
        assertEquals(64, snapshot.operations.size)
        assertEquals(65L, snapshot.storeRevision)
        assertTrue(snapshot.spending.grossExpense.units > 0L)
        fixture.gateway.close()
    }
}
