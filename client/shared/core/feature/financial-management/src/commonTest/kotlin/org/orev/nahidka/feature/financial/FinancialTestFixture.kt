package org.orev.nahidka.feature.financial

import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.YearMonth
import org.orev.nahidka.feature.financial.calculation.FinancialCalendar
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialCategory
import org.orev.nahidka.feature.financial.dto.FinancialOperation
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.gateway.InMemoryFinancialGateway
import org.orev.nahidka.feature.financial.support.NoOpFinancialErrorReporter

internal class FinancialTestFixture(
    journalCapacity: Int = 32,
    receiptCapacity: Int = 32,
) {
    val usd = AssetDefinition("iso4217:USD", "USD", 2)
    val config = FinancialSessionConfig(
        sessionIdentity = "test-session",
        workspaceIdentity = "test-workspace",
        assets = persistentListOf(usd),
        defaultAssetId = usd.id,
        reportingTimeZone = "Europe/Kyiv",
        journalCapacity = journalCapacity,
        commandReceiptCapacity = receiptCapacity,
    )
    val gateway = InMemoryFinancialGateway(config, NoOpFinancialErrorReporter(), FinancialCalendar())
    val month = YearMonth(2024, 5)
    val occurredAt = Instant.parse("2024-05-12T10:30:00Z")

    suspend fun category(id: String = "food", name: String = "Food"): FinancialCategory {
        val result = gateway.createCategory(CreateFinancialCategory(CommandMeta("create-category-$id"), id, name))
        return (result as MutationResult.Committed).value
    }

    suspend fun operation(
        id: String,
        categoryId: String?,
        units: Long,
        kind: OperationKind = OperationKind.EXPENSE,
        paymentMethod: PaymentMethod = PaymentMethod.CARD,
        refundOf: String? = null,
        at: Instant = occurredAt,
        commandId: String = "add-$id",
        description: String? = null,
    ): FinancialOperation {
        val result = gateway.addOperation(
            AddFinancialOperation(
                CommandMeta(commandId),
                NewFinancialOperation(
                    id = id,
                    amount = Money(usd.id, units),
                    kind = kind,
                    categoryId = categoryId,
                    paymentMethod = paymentMethod,
                    occurredAt = at,
                    description = description,
                    refundOfOperationId = refundOf,
                ),
            ),
        )
        return (result as MutationResult.Committed).value
    }

    fun query(assetId: String = usd.id) = MonthlyQuery(month, assetId)
}
