package org.orev.nahidka.feature.financial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import org.orev.nahidka.feature.financial.calculation.parseMoneyText
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.DeleteFinancialCategory
import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.command.NullablePatch
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter

class FinancialLedgerTest {
    @Test
    fun exactMoneyParsingRejectsAmbiguousOrExcessPrecision() {
        val usd = AssetDefinition("iso4217:USD", "USD", 2)
        assertEquals(Money(usd.id, 123_456), parseMoneyText("1,234.56", usd))
        assertEquals(Money(usd.id, 120), parseMoneyText("1.2", usd))
        assertNull(parseMoneyText("1.234", usd))
        assertNull(parseMoneyText("1,23.00", usd))
        assertNull(parseMoneyText("1e3", usd))
        assertNull(parseMoneyText("92233720368547758.08", usd))
        assertEquals("-92233720368547758.07 USD", ExactMoneyFormatter().format(Money(usd.id, Long.MIN_VALUE + 1), usd))
    }

    @Test
    fun commandRetriesAreIdempotentAndVersionConflictsKeepCanonicalState() = kotlinx.coroutines.test.runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        val add = AddFinancialOperation(
            CommandMeta("add-command"),
            NewFinancialOperation("ledger-op", Money(fixture.usd.id, 1_000), OperationKind.EXPENSE, category.id, PaymentMethod.CARD, fixture.occurredAt),
        )
        val first = assertIs<MutationResult.Committed<FinancialOperation>>(fixture.gateway.addOperation(add))
        val retry = assertIs<MutationResult.Committed<FinancialOperation>>(fixture.gateway.addOperation(add))
        assertEquals(first, retry)
        assertEquals(2L, first.storeRevision)
        assertEquals(true, retry.changed)

        val reused = fixture.gateway.addOperation(add.copy(operation = add.operation.copy(amount = Money(fixture.usd.id, 2_000))))
        assertIs<FinancialError.CommandIdReused>((reused as MutationResult.Rejected).error)

        val update = UpdateFinancialOperation(
            CommandMeta("clear-description"),
            first.value.id,
            first.value.version,
            FinancialOperationPatch(description = NullablePatch.Set("memo")),
        )
        val changed = assertIs<MutationResult.Committed<FinancialOperation>>(fixture.gateway.updateOperation(update))
        assertEquals(2L, changed.value.version)
        val clear = fixture.gateway.updateOperation(
            UpdateFinancialOperation(
                CommandMeta("clear-description-field"),
                first.value.id,
                changed.value.version,
                FinancialOperationPatch(description = NullablePatch.Clear),
            ),
        )
        assertNull(assertIs<MutationResult.Committed<FinancialOperation>>(clear).value.description)
        val conflict = fixture.gateway.updateOperation(update.copy(meta = CommandMeta("stale-update"))) as MutationResult.Rejected
        assertIs<FinancialError.OperationConflict>(conflict.error)
        fixture.gateway.close()
    }

    @Test
    fun refundsInheritExpenseCategoryAndCannotExceedTheOriginal() = kotlinx.coroutines.test.runTest {
        val fixture = FinancialTestFixture()
        val food = fixture.category()
        val expense = fixture.operation("expense", food.id, 1_000)
        val refund = fixture.gateway.addOperation(
            AddFinancialOperation(
                CommandMeta("refund-command"),
                NewFinancialOperation("refund", Money(fixture.usd.id, 1_000), OperationKind.REFUND, null, PaymentMethod.CASH, fixture.occurredAt, refundOfOperationId = expense.id),
            ),
        )
        assertEquals(food.id, assertIs<MutationResult.Committed<FinancialOperation>>(refund).value.categoryId)

        val excess = fixture.gateway.addOperation(
            AddFinancialOperation(
                CommandMeta("excess-refund-command"),
                NewFinancialOperation("excess-refund", Money(fixture.usd.id, 1), OperationKind.REFUND, null, PaymentMethod.CASH, fixture.occurredAt, refundOfOperationId = expense.id),
            ),
        ) as MutationResult.Rejected
        assertIs<FinancialError.Validation>(excess.error)
        val cannotDeleteParent = fixture.gateway.removeOperation(RemoveFinancialOperation(CommandMeta("remove-parent"), expense.id, expense.version)) as MutationResult.Rejected
        assertIs<FinancialError.Validation>(cannotDeleteParent.error)
        fixture.gateway.close()
    }

    @Test
    fun categoryNamesAreUniqueAndReferencedCategoriesMustBeArchived() = kotlinx.coroutines.test.runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category("food", "Food")
        val duplicate = fixture.gateway.createCategory(CreateFinancialCategory(CommandMeta("duplicate"), "food-2", "  FOOD  ")) as MutationResult.Rejected
        assertIs<FinancialError.Validation>(duplicate.error)
        fixture.operation("expense", category.id, 1_000)
        val inUse = fixture.gateway.deleteCategory(DeleteFinancialCategory(CommandMeta("delete-used"), category.id, category.version)) as MutationResult.Rejected
        assertIs<FinancialError.CategoryInUse>(inUse.error)
        val archived = assertIs<MutationResult.Committed<FinancialCategory>>(
            fixture.gateway.archiveCategory(ArchiveFinancialCategory(CommandMeta("archive"), category.id, category.version)),
        )
        assertEquals(true, archived.value.archived)
        fixture.gateway.close()
    }

    @Test
    fun aggregateOverflowRejectsTheWholeCommandAndNoOpDoesNotAdvanceRevision() = kotlinx.coroutines.test.runTest {
        val fixture = FinancialTestFixture()
        val category = fixture.category()
        val expense = fixture.operation("maximum", category.id, Long.MAX_VALUE)
        val noOp = fixture.gateway.updateOperation(
            UpdateFinancialOperation(CommandMeta("no-op"), expense.id, expense.version, FinancialOperationPatch()),
        )
        val noOpReceipt = assertIs<MutationResult.Committed<FinancialOperation>>(noOp)
        assertEquals(false, noOpReceipt.changed)
        assertEquals(2L, noOpReceipt.storeRevision)

        val overflow = fixture.gateway.addOperation(
            AddFinancialOperation(
                CommandMeta("overflow-command"),
                NewFinancialOperation("overflow", Money(fixture.usd.id, 1), OperationKind.EXPENSE, category.id, PaymentMethod.CASH, fixture.occurredAt),
            ),
        ) as MutationResult.Rejected
        assertIs<FinancialError.Validation>(overflow.error)
        val snapshot = fixture.gateway.observeFinancialSnapshot(fixture.query()).first()
        assertEquals(2L, snapshot.storeRevision)
        assertEquals(listOf(expense), snapshot.operations)
        fixture.gateway.close()
    }
}
