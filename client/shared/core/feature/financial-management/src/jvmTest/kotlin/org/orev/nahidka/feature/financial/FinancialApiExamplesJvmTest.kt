package org.orev.nahidka.feature.financial

import dev.zacsweers.metro.createGraphFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.OperationQuery
import org.orev.nahidka.feature.financial.dto.PaymentMethod

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialApiExamplesJvmTest {
    @Test
    fun documentedGraphRunsCommandsAndReplaysOperationChanges() = runTest {
        val fixture = FinancialTestFixture()
        val finance = createGraphFactory<FinancialApiExampleGraph.Factory>().create(fixture.gateway).finance
        var generatedId = 0
        fun nextId(): String = "example-${++generatedId}"
        val changes = mutableListOf<String>()
        var job: Job? = null
        try {
            val category = committed(
                finance.financialCategoryManager.createCategory(
                    CreateFinancialCategory(CommandMeta(nextId()), nextId(), "Groceries", "cart"),
                ),
            )
            job = finance.financialContext.subscribe(OperationQuery(assetId = fixture.usd.id))
                .onSnapshot { snapshot -> changes += "snapshot:${snapshot.entities.size}" }
                .onInsert { change -> changes += "insert:${change.after.id}" }
                .onError { throw it }
                .launchIn(backgroundScope)
            runCurrent()

            val add = AddFinancialOperation(
                meta = CommandMeta(nextId()),
                operation = NewFinancialOperation(
                    id = nextId(),
                    amount = Money(fixture.usd.id, 1_234),
                    kind = OperationKind.EXPENSE,
                    categoryId = category.value.id,
                    paymentMethod = PaymentMethod.CARD,
                    occurredAt = fixture.occurredAt,
                    description = "Groceries",
                ),
            )
            val created = committed(finance.financialManager.addNewFinancialManipulation(add))
            runCurrent()
            assertEquals(listOf("snapshot:0", "insert:${created.value.id}"), changes)
            val retried = committed(finance.financialManager.addNewFinancialManipulation(add))
            assertEquals(created.value, retried.value)
            assertEquals(2L, retried.storeRevision)
        } finally {
            job?.cancelAndJoin()
            fixture.gateway.close()
        }
    }

    private fun <T> committed(result: MutationResult<T>): MutationResult.Committed<T> =
        assertIs<MutationResult.Committed<T>>(result)
}
