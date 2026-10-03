package org.orev.nahidka.feature.financial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.OperationQuery
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.subscription.FinancialSessionClosedException

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialSubscriptionTest {
    @Test
    fun initialSnapshotPrecedesTheCommittedInsertBatch() = runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        val messages = mutableListOf<String>()
        val job = fixture.gateway.subscribeOperations(OperationQuery())
            .onSnapshot { messages += "snapshot:${it.entities.size}" }
            .onInsert { messages += "insert:${it.after.id}" }
            .onUpdate { messages += "update:${it.after.id}" }
            .launchIn(backgroundScope)
        runCurrent()
        assertEquals(listOf("snapshot:0"), messages)

        val operation = fixture.operation("observed", category.id, 100)
        runCurrent()
        assertEquals(listOf("snapshot:0", "insert:${operation.id}"), messages)
        job.cancel()
        fixture.gateway.close()
    }

    @Test
    fun retainedInsertUpdateDeleteBurstIsReplayedInCommitOrder() = runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        val changes = mutableListOf<String>()
        val job = fixture.gateway.subscribeOperations(OperationQuery())
            .onSnapshot { }
            .onInsert { changes += "insert:${it.after.id}" }
            .onUpdate { changes += "update:${it.after.id}" }
            .onDelete { changes += "delete:${it.before.id}" }
            .launchIn(backgroundScope)
        runCurrent()
        val operation = fixture.operation("burst", category.id, 100)
        val updated = fixture.gateway.updateOperation(
            UpdateFinancialOperation(CommandMeta("burst-update"), operation.id, operation.version, FinancialOperationPatch(amount = Money(fixture.usd.id, 200))),
        ) as MutationResult.Committed
        fixture.gateway.removeOperation(RemoveFinancialOperation(CommandMeta("burst-delete"), operation.id, updated.value.version))
        runCurrent()
        assertEquals(listOf("insert:burst", "update:burst", "delete:burst"), changes)
        job.cancel()
        fixture.gateway.close()
    }

    @Test
    fun boundedJournalOverflowReplacesSubscriberStateWithCurrentResync() = runTest {
        val fixture = FinancialTestFixture(journalCapacity = 2)
        val category = fixture.category()
        val initialEntered = CompletableDeferred<Unit>()
        val releaseInitial = CompletableDeferred<Unit>()
        val snapshots = mutableListOf<Int>()
        val job = fixture.gateway.subscribeOperations(OperationQuery())
            .onSnapshot {
                snapshots += it.entities.size
                initialEntered.complete(Unit)
                releaseInitial.await()
            }
            .onResync { snapshots += it.entities.size }
            .launchIn(backgroundScope)
        runCurrent()
        initialEntered.await()
        repeat(4) { index -> fixture.operation("overflow-$index", category.id, 100L + index) }
        releaseInitial.complete(Unit)
        runCurrent()
        assertEquals(listOf(0, 4), snapshots)
        job.cancel()
        fixture.gateway.close()
    }

    @Test
    fun aCallbackCanSubmitAnotherCommandWithoutHoldingTheStoreLock() = runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        val inserted = mutableListOf<String>()
        val completed = CompletableDeferred<Unit>()
        val job = fixture.gateway.subscribeOperations(OperationQuery())
            .onSnapshot { }
            .onInsert { change ->
                inserted += change.after.id
                if (change.after.id == "first") {
                    fixture.operation("second", category.id, 200)
                } else if (change.after.id == "second") {
                    completed.complete(Unit)
                }
            }
            .launchIn(backgroundScope)
        runCurrent()
        fixture.operation("first", category.id, 100)
        runCurrent()
        completed.await()
        assertEquals(listOf("first", "second"), inserted)
        job.cancel()
        fixture.gateway.close()
    }

    @Test
    fun cancellingOneSubscriptionDoesNotAffectSiblingSubscribers() = runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        var cancelledSubscriberChanges = 0
        var siblingChanges = 0
        val cancelled = fixture.gateway.subscribeOperations(OperationQuery())
            .onSnapshot { }
            .onInsert { cancelledSubscriberChanges++ }
            .launchIn(backgroundScope)
        val sibling = fixture.gateway.subscribeOperations(OperationQuery())
            .onSnapshot { }
            .onInsert { siblingChanges++ }
            .launchIn(backgroundScope)
        runCurrent()
        cancelled.cancel()
        fixture.operation("surviving", category.id, 100)
        runCurrent()
        assertEquals(0, cancelledSubscriberChanges)
        assertEquals(1, siblingChanges)
        assertTrue(sibling.isActive)
        sibling.cancel()
        fixture.gateway.close()
    }

    @Test
    fun sessionCloseClearsReadsRejectsCommandsAndEndsWatchers() = runTest {
        val fixture = FinancialTestFixture()
        val snapshots = mutableListOf<Int>()
        val job = fixture.gateway.subscribeOperations(OperationQuery())
            .onSnapshot { snapshots += it.entities.size }
            .launchIn(backgroundScope)
        runCurrent()
        assertEquals(listOf(0), snapshots)
        fixture.gateway.close()
        job.join()
        assertFalse(job.isActive)
        val closed = fixture.gateway.addOperation(
            AddFinancialOperation(
                CommandMeta("after-close"),
                NewFinancialOperation("after-close", Money(fixture.usd.id, 100), OperationKind.INCOME, null, PaymentMethod.CASH, fixture.occurredAt),
            ),
        ) as MutationResult.Rejected
        assertEquals(FinancialError.SessionClosed, closed.error)
        assertFailsWith<FinancialSessionClosedException> { fixture.gateway.observeFinancialSnapshot(fixture.query()).first() }
    }
}
