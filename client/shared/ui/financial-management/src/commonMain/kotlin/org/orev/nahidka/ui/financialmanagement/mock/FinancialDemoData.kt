package org.orev.nahidka.ui.financialmanagement.mock

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.feature.financial.calculation.financialMonthFor
import org.orev.nahidka.feature.financial.command.*
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.FinancialGateway

object FinancialDemoData {

    val sessionConfig = FinancialSessionConfig(
        sessionIdentity = "demo-session",
        workspaceIdentity = "demo-workspace",
        assets = persistentListOf(
            AssetDefinition("iso4217:USD", "USD", 2),
            AssetDefinition("iso4217:UAH", "UAH", 2),
        ),
        defaultAssetIdentifier = "iso4217:USD",
        reportingTimeZone = "Europe/Kyiv",
    )

    suspend fun populate(gateway: FinancialGateway, clock: ApplicationClock) {
        val categories = listOf(
            Triple("demo-food", "Food", "🍔"),
            Triple("demo-home", "Home", "🏠"),
            Triple("demo-transport", "Transport", "🚆"),
            Triple("demo-leisure", "Leisure", "🎬"),
            Triple("demo-salary", "Salary", "💼"),
        )
        categories.forEach { category ->
            gateway.createCategory(
                CreateFinancialCategory(CommandMeta("seed-${category.first}"), category.first, category.second, category.third),
            ).requireCommitted()
        }

        val month = financialMonthFor(clock.now(), sessionConfig.reportingTimeZone)
        val timeZone = TimeZone.of(sessionConfig.reportingTimeZone)
        val occurredAt = LocalDate(month.year, month.month, 1)
            .atTime(12, 0)
            .toInstant(timeZone)

        sessionConfig.assets.forEach { asset ->
            val multiplier = if (asset.displayCode == "UAH") 40L else 1L
            val incomeIdentifier = "demo-income-${asset.displayCode}"
            gateway.addOperation(
                AddFinancialOperation(
                    CommandMeta("seed-$incomeIdentifier"),
                    NewFinancialOperation(
                        identifier = incomeIdentifier,
                        amount = Money(asset.identifier, 300_000L * multiplier),
                        kind = OperationKind.INCOME,
                        categoryIdentifier = "demo-salary",
                        paymentMethod = PaymentMethod.CARD,
                        occurredAt = occurredAt,
                    ),
                ),
            ).requireCommitted()

            repeat(55) { operationIndex ->
                val operationIdentifier = "demo-expense-${asset.displayCode}-$operationIndex"
                gateway.addOperation(
                    AddFinancialOperation(
                        CommandMeta("seed-$operationIdentifier"),
                        NewFinancialOperation(
                            identifier = operationIdentifier,
                            amount = Money(asset.identifier, (500L + operationIndex * 25L) * multiplier),
                            kind = OperationKind.EXPENSE,
                            categoryIdentifier = categories[operationIndex % 4].first,
                            paymentMethod = PaymentMethod.entries[operationIndex % PaymentMethod.entries.size],
                            occurredAt = occurredAt,
                        ),
                    ),
                ).requireCommitted()
            }

            gateway.savePlanningTable(
                SavePlanningTable(
                    meta = CommandMeta("seed-plan-${asset.displayCode}"),
                    expectedVersion = null,
                    table = FinancialPlanningTableInput(
                        identifier = "demo-plan-${asset.displayCode}",
                        month = month,
                        assetIdentifier = asset.identifier,
                        openingAvailable = Money(asset.identifier, 0),
                        savingsPolicy = null,
                        rows = categories.take(3)
                            .mapIndexed { categoryIndex, category ->
                                PlanningRow(
                                    identifier = "demo-row-${asset.displayCode}-$categoryIndex",
                                    categoryIdentifier = category.first,
                                    plannedAmount = Money(asset.identifier, (40_000L + categoryIndex * 10_000L) * multiplier),
                                    included = true,
                                    preferredPaymentMethod = PaymentMethod.CARD,
                                )
                            }
                            .toPersistentList(),
                    ),
                ),
            ).requireCommitted()
        }
    }

    private fun MutationResult<*>.requireCommitted() {
        check(this is MutationResult.Committed) { "Could not initialize financial demo data: $this" }
    }
}
