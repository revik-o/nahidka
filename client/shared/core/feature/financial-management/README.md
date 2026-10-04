# Financial management

Exact money, operation/category CRUD, monthly planning, and observable projections for one in-memory session. Package root: `org.orev.nahidka.feature.financial`.

**Connect**

```kotlin
// Consumer build.gradle.kts — Android, JVM, JS, Wasm JS, iOS arm64/simulator arm64.
commonMain.dependencies {
    implementation(project(":shared:core:feature:financial-management"))
}
```

The usage fragments below share these imports/setup; run suspending calls in an owner coroutine.

```kotlin
import kotlin.time.Instant
import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.YearMonth
import org.orev.nahidka.core.common.*
import org.orev.nahidka.feature.financial.calculation.*
import org.orev.nahidka.feature.financial.command.*
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.dto.*
import org.orev.nahidka.feature.financial.gateway.*
import org.orev.nahidka.feature.financial.subscription.FinancialSessionClosedException
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter

val usd = AssetDefinition("iso4217:USD", "USD", fractionDigits = 2)
val config = FinancialSessionConfig(
    sessionIdentity = "session-1", workspaceIdentity = "workspace-1",
    assets = persistentListOf(usd), defaultAssetIdentifier = usd.identifier,
    reportingTimeZone = "Europe/Kyiv",
)
val calendar = FinancialCalendar()
val ids = RandomIdentifierGenerator()
val clock = SystemApplicationClock()
val gateway: FinancialGateway = InMemoryFinancialGateway(
    config, ErrorReporter { failure -> println(failure.message) }, calendar,
)
val finance = FinancialModule(gateway) // finance.financialGateway === gateway
val month = YearMonth(2026, 10)

// Config defaults; all capacities/limits must be positive:
// journalCapacity=256, commandReceiptCapacity=512, maxOperations=100_000,
// maxCategories=512, maxPlanningTables=1_000, maxPlanningRows=500,
// maxDescriptionLength=2_000, maxCategoryNameLength=80.
// Assets: unique nonblank identifier, nonblank displayCode, fractionDigits in 0..18.
// defaultAssetIdentifier must exist; use a valid IANA reportingTimeZone.
// Each gateway starts empty; identity strings do not open persistent storage.
```

```kotlin
// Lifecycle example. Use the application's lifecycle scope for long-lived work.
suspend fun runSessionWork(gateway: FinancialGateway) {
    try {
        val snapshot = gateway.observeFinancialSnapshot(MonthlyQuery(month, usd.identifier)).first()
        println(snapshot.spending.netExpense)
    } finally {
        withContext(NonCancellable) { gateway.close() }
    }
}
// close(): idempotent; clears records/receipts, cancels subscriptions,
// rejects later commands with FinancialError.SessionClosed.
// Raw snapshot Flow terminates with FinancialSessionClosedException on close.
// Metro: FinancialModule is @SingleIn(FinancialSessionScope::class).
// Host graph binds FinancialGateway to a session-scoped InMemoryFinancialGateway;
// merely constructing FinancialModule does not establish or own that lifecycle.
```

**Every gateway entry point** — signature inventory; imports are shown above.

```kotlin
// suspend mutations -> MutationResult<FinancialOperation>
addOperation(command: AddFinancialOperation)
updateOperation(command: UpdateFinancialOperation)
removeOperation(command: RemoveFinancialOperation)
// suspend mutations -> MutationResult<FinancialCategory>
createCategory(command: CreateFinancialCategory)
updateCategory(command: UpdateFinancialCategory)
archiveCategory(command: ArchiveFinancialCategory)
deleteCategory(command: DeleteFinancialCategory)
// suspend mutations -> MutationResult<PlanningTableView>
savePlanningTable(command: SavePlanningTable)
deletePlanningTable(command: DeletePlanningTable)
// reads
subscribeOperations(query: OperationQuery): FinancialSubscription<FinancialOperation>
subscribeCategories(query: CategoryQuery): FinancialSubscription<FinancialCategory>
subscribePlanning(query: PlanningQuery): FinancialSubscription<PlanningTableView>
observeFinancialSnapshot(query: MonthlyQuery): Flow<FinancialSnapshot>
suspend close(): Unit
// No separate get/list API: use a subscription or snapshotFlow.first().
```

**Create, edit, refund, remove**

```kotlin
suspend fun createFoodCategory(gateway: FinancialGateway): MutationResult<FinancialCategory> =
    gateway.createCategory(CreateFinancialCategory(
        meta = CommandMeta(ids.next()), identifier = ids.next(), name = "Food", iconName = "restaurant",
    ))

suspend fun addLunch(gateway: FinancialGateway, category: FinancialCategory): MutationResult<FinancialOperation> =
    gateway.addOperation(AddFinancialOperation(
        meta = CommandMeta(ids.next()),
        operation = NewFinancialOperation(
            identifier = ids.next(), amount = Money(usd.identifier, 1_250), // USD 12.50
            kind = OperationKind.EXPENSE, categoryIdentifier = category.identifier,
            paymentMethod = PaymentMethod.CARD, occurredAt = clock.now(), description = "Lunch",
        ),
    ))

suspend fun editDescription(gateway: FinancialGateway, current: FinancialOperation) =
    gateway.updateOperation(UpdateFinancialOperation(
        CommandMeta(ids.next()), current.identifier, current.version,
        FinancialOperationPatch(description = NullablePatch.Set("Team lunch")),
    ))
// FinancialOperation exposes NewFinancialOperation fields plus version: Long.
// Patch fields: amount: Money?; paymentMethod: PaymentMethod?; occurredAt: Instant?
//               categoryIdentifier, description, refundOfOperationIdentifier: NullablePatch<String>
// null scalar = keep; NullablePatch.Keep = keep; Set(value) = replace; Clear = null.
// kind is immutable. Expense category cannot be cleared.

suspend fun refundLunch(gateway: FinancialGateway, expense: FinancialOperation) =
    gateway.addOperation(AddFinancialOperation(CommandMeta(ids.next()), NewFinancialOperation(
        identifier = ids.next(), amount = Money(expense.amount.assetIdentifier, 250),
        kind = OperationKind.REFUND, categoryIdentifier = null, // inherited on creation
        paymentMethod = PaymentMethod.CARD, occurredAt = clock.now(),
        refundOfOperationIdentifier = expense.identifier,
    )))

suspend fun removeOperation(gateway: FinancialGateway, current: FinancialOperation) =
    gateway.removeOperation(RemoveFinancialOperation(CommandMeta(ids.next()), current.identifier, current.version))
// Amounts must be > 0 for INCOME, EXPENSE, REFUND; kind determines the sign in totals.
// INCOME may have no category; EXPENSE requires one.
// REFUND requires an existing expense with the same asset and inherited category.
// All refunds together <= expense amount, including refunds posted in other months.
// Delete linked refunds before deleting their expense. While refunds exist, the
// expense cannot change asset/category or shrink below the total refunded amount.
// PaymentMethod: CASH, CARD, CRYPTOCURRENCY (independent of amount.assetIdentifier).
```

```kotlin
suspend fun renameCategory(gateway: FinancialGateway, category: FinancialCategory) =
    gateway.updateCategory(UpdateFinancialCategory(
        CommandMeta(ids.next()), category.identifier, category.version,
        name = "Groceries", iconName = NullablePatch.Clear,
    ))
suspend fun archiveCategory(gateway: FinancialGateway, category: FinancialCategory) =
    gateway.archiveCategory(ArchiveFinancialCategory(CommandMeta(ids.next()), category.identifier, category.version))
suspend fun deleteCategory(gateway: FinancialGateway, category: FinancialCategory) =
    gateway.deleteCategory(DeleteFinancialCategory(CommandMeta(ids.next()), category.identifier, category.version))
// FinancialCategory fields: identifier, version, name, iconName, archived.
// Each command is independent: refresh category.version after a successful change.
// Names are trimmed and unique ignoring case, including archived categories.
// iconName <= 64 chars. Archive has no reverse command.
// Archived references can be retained on existing records; new operations/planning
// rows cannot select them (refunds may inherit an archived expense category).
// Delete rejects CategoryInUse if any operation or planning row references it.
```

**Handle results, versions, retries**

```kotlin
fun <T> logResult(result: MutationResult<T>) {
    when (result) {
        is MutationResult.Committed -> println("${result.value}; revision=${result.storeRevision}; changed=${result.changed}")
        is MutationResult.Rejected -> when (val error = result.error) {
            is FinancialError.Validation -> println("${error.field}: ${error.message}")
            is FinancialError.NotFound -> println("${error.entity}: ${error.identifier}")
            is FinancialError.OperationConflict -> println("Review latest operation: ${error.current}")
            is FinancialError.CategoryConflict -> println("Review latest category: ${error.current}")
            is FinancialError.PlanningConflict -> println("Review latest table: ${error.current}")
            is FinancialError.CategoryInUse -> println("Still referenced: ${error.identifier}")
            is FinancialError.CommandIdentifierReused -> println("New payload needs a new command ID")
            FinancialError.SessionClosed -> println("Create a new session")
            FinancialError.StorageUnavailable, FinancialError.Forbidden -> println(error)
        }
    }
}
// expectedVersion = entity.version, never snapshot.storeRevision.
// New entity versions start at next store revision; edits increment entity version.
// Reusing a deleted entity ID cannot make an old version valid for its replacement.
// No-op edit: Committed(changed=false), no version/revision increment or event.
// Remove/delete return the removed entity/view in Committed.value.
// Retry exactly the same command object/ID => retained committed result, no duplicate mutation.
// Same retained command ID + different payload => CommandIdentifierReused.
// Receipt cache is bounded; only committed results are cached. A retry is not
// permanently deduplicated after eviction. New user intent => new command ID.
// IDs for commands and newly created entities/rows: nonblank, <=128 chars.
// Overflow during mutation => Validation; standalone arithmetic helpers throw.
```

**Observe coherent monthly data or entity changes**

```kotlin
suspend fun readMonth(gateway: FinancialGateway): FinancialSnapshot =
    gateway.observeFinancialSnapshot(MonthlyQuery(month, usd.identifier)).first()

fun watchMonth(gateway: FinancialGateway, ownerScope: CoroutineScope): Job =
    gateway.observeFinancialSnapshot(MonthlyQuery(month, usd.identifier))
        .onEach { snapshot ->
            println(snapshot.spending.slices) // totals, rows, operations share one revision
            when (val plan = snapshot.planning) {
                is PlanningConfigured -> println(plan.table.totals.currentAvailable)
                is PlanningNotConfigured -> println(plan.netExpense)
            }
        }
        .catch { failure -> if (failure !is FinancialSessionClosedException) println(failure) }
        .launchIn(ownerScope)
// Snapshot fields: sessionIdentity, storeRevision, period, asset, operations,
//                  categories (including archived), spending, planning.
// Snapshot Flow can conflate intermediate commits; it is a current-state read.
// Unknown monthly asset => IllegalArgumentException during collection.

fun watchExpenses(gateway: FinancialGateway, ownerScope: CoroutineScope): Job {
    val byId = mutableMapOf<String, FinancialOperation>()
    return gateway.subscribeOperations(OperationQuery(
        period = calendar.reportingPeriod(month, config.reportingTimeZone),
        assetIdentifier = usd.identifier, kinds = persistentSetOf(OperationKind.EXPENSE),
    ))
        .onSnapshot { snapshot -> // REQUIRED; also default handler for a resync
            byId.clear()
            snapshot.entities.forEach { byId[it.identifier] = it }
        }
        .onInsert { byId[it.after.identifier] = it.after }
        .onUpdate { byId[it.after.identifier] = it.after }
        .onDelete { byId.remove(it.before.identifier) }
        .onError { failure -> println(failure) }
        .launchIn(ownerScope) // scope must contain a lifecycle-owned Job
}
// Builder methods return a NEW FinancialSubscription<T>; chain/use the return value.
// Optional .onResync { EntitySnapshot<T> -> ... }: replace the ENTIRE local cache.
// Optional .onBatch { FinancialChangeBatch<T> -> ... }: runs BEFORE per-entity handlers.
// Choose batch or entity callbacks to apply changes once. Handlers run serially.
// Slow observers replay the bounded journal, then resync if older commits expired.
// Handler failure ends this subscription; its supervisor isolates sibling jobs.
// Cancel returned Job or owner scope; gateway.close() also cancels subscriptions.
```

```kotlin
// Query/envelope inventory (abbreviated declarations, not executable statements):
OperationQuery(period: ReportingPeriod? = null, assetIdentifier: String? = null,
               categoryIdentifier: String? = null, kinds: PersistentSet<OperationKind> = persistentSetOf())
CategoryQuery(includeArchived: Boolean = true)
PlanningQuery(month: YearMonth? = null, assetIdentifier: String? = null)
MonthlyQuery(month: YearMonth, assetIdentifier: String)
// Null query field/empty kinds = no filter; null category is NOT "uncategorized only".
// Query membership changes produce Insert/Delete, even when the entity was updated.
// Snapshot order: operations = occurredAt descending, then identifier;
// categories = name ignoring case, then identifier; planning = month, asset, identifier.
// Re-sort UI lists yourself after applying deltas (FinancialOperationRecencyComparator,
// FinancialCategoryNameComparator reproduce the snapshot orders). Change.index is a commit delta
// index, NOT a position in the query result.
EntitySnapshot<T>(sessionIdentity: String, storeRevision: Long, entities: PersistentList<T>)
FinancialChangeBatch<T>(storeRevision: Long, commandIdentifier: String, changes: PersistentList<EntityChange<T>>)
// EntityChange.Insert(after), Update(before, after), Delete(before)
// all expose storeRevision: Long, commandIdentifier: String, index: Int.
// WatchMessage<T>: Snapshot(snapshot), Resync(snapshot), Batch(batch).
// WatchMessage is public, but the gateway exposes handlers rather than its raw Flow.
```

**Plan a month**

```kotlin
suspend fun createPlan(gateway: FinancialGateway, category: FinancialCategory) =
    gateway.savePlanningTable(SavePlanningTable(
        meta = CommandMeta(ids.next()), expectedVersion = null, // null = CREATE only
        table = FinancialPlanningTableInput(
            identifier = ids.next(), month = month, assetIdentifier = usd.identifier,
            openingAvailable = Money(usd.identifier, 100_000),
            savingsPolicy = SavingsGuidelinePolicy(Money(usd.identifier, 20_000), 2_500), // reserve, 25%
            rows = persistentListOf(PlanningRowInput(
                identifier = ids.next(), categoryIdentifier = category.identifier,
                plannedAmount = Money(usd.identifier, 30_000), included = true,
                preferredPaymentMethod = PaymentMethod.CARD,
            )),
        ),
    ))

suspend fun changeOpeningFunds(gateway: FinancialGateway, view: PlanningTableView) =
    gateway.savePlanningTable(SavePlanningTable(
        CommandMeta(ids.next()), expectedVersion = view.document.version,
        table = view.document.toInput().copy(openingAvailable = Money(view.document.assetIdentifier, 120_000)),
    ))

suspend fun deletePlan(gateway: FinancialGateway, view: PlanningTableView) =
    gateway.deletePlanningTable(DeletePlanningTable(
        CommandMeta(ids.next()), view.document.identifier, view.document.version,
    ))
// One table per (month, assetIdentifier); update cannot change month/asset.
// Save replaces the whole input, including rows. PlanningRowInput is a typealias of PlanningRow.
// Row IDs must be unique across active tables; category appears once per table.
// openingAvailable may be signed; plannedAmount/reserve >=0, all use table asset.
// allocationBasisPoints in 0..10_000; savingsPolicy=null => suggestedSavings=null.
// preferredPaymentMethod is metadata: actuals include every payment method.
// included=false excludes the row from budget/reservation, but retains its actuals.
// document.toInput() and input.toPlanningTable(version) are pure mappings, not writes.
// Planning subscription updates also follow affected operations/category renames,
// even when view.document.version stays the same. No event if the view is equal.
```

```text
PlanningTableView = document: FinancialPlanningTable + rows: PersistentList<PlanningRowView> + totals: PlanningTotals
FinancialPlanningTable = version, identifier, month, assetIdentifier, openingAvailable, savingsPolicy, rows
PlanningRow = identifier, categoryIdentifier, plannedAmount, included, preferredPaymentMethod
PlanningRowView = input, categoryName, grossSpent, refunded, netSpent, remainingBudget, overspent,
                 actualByPaymentMethod: PersistentMap<PaymentMethod, PaymentActuals>
PaymentActuals = grossSpent, refunded, netSpent (Money)

netSpent                        = grossSpent - refunded
remainingBudget                 = max(plannedAmount - max(netSpent, 0), 0)
overspent                       = max(max(netSpent, 0) - plannedAmount, 0)
fundsAvailableThisMonth          = openingAvailable + income
currentAvailable                = fundsAvailableThisMonth - grossExpenses + refunds
includedBudget                  = sum(included rows' plannedAmount)
remainingReservation            = sum(included rows' remainingBudget)
projectedAvailableAfterPlanning  = currentAvailable - remainingReservation
unplannedNetExpense             = expense - refunds for categories absent from ALL rows
suggestedSavings                = floor(max(projectedAvailableAfterPlanning - reserve, 0) * rate / 10_000)

PlanningState = PlanningConfigured(table: PlanningTableView) | PlanningNotConfigured
PlanningNotConfigured = period, income, grossExpenses, refunds, netExpense, currentAvailable
SpendingSummary = storeRevision, period, assetIdentifier, grossExpense, refunds, netExpense,
                  drawableTotal, refundCredits, slices: PersistentList<SpendingSlice>
SpendingSlice = categoryIdentifier, label, amount, percentageBasisPoints, isOtherGroup
// Spending slices show positive category nets; negative nets become refundCredits.
// drawableTotal - refundCredits = netExpense. Nonempty slices sum to 10_000 basis points.
// maxSlices defaults to 8; overflow categories merge into "Other" (projection:other).
// Every monthly calculation uses [startInclusive, endExclusive) in the reporting zone.
// Refunds affect their own occurrence month, which can differ from the expense month.
```

**Exact amounts and standalone calculations**

```kotlin
val amount = parseMoneyText("1,234.50", usd) // Money("iso4217:USD", 123450)
val localized = parseMoneyText("1.234,50", usd, decimalSeparator = ',', groupingSeparator = '.')
val invalid = parseMoneyText("12.345", usd) // null: more than 2 fraction digits
val input = formatMoneyInput(Money(usd.identifier, -125), usd) // "-1.25"
val label = ExactMoneyFormatter().format(Money(usd.identifier, 125), usd) // "1.25 USD"
val sum = checkedMoneyAdd(Money(usd.identifier, 100), Money(usd.identifier, 25))
val difference = checkedMoneySubtract(sum, Money(usd.identifier, 10))
val unitsSum = checkedAdd(100, 25) // Long; checkedSubtract(left, right) also exported
val allocation = allocateBasisPoints(units = 999, rate = 2_500) // 249, floor rounding
val share = divideShare(numerator = 1, denominator = 3) // ShareDivision(3333, 1)
// Money is signed Long minor units; parsing/formatting supports the entire Long range.
// Parsing returns null for invalid grouping, precision, or range; no floating point.
// Money arithmetic/formatting require matching assets; no currency conversion.
// checked* overflow throws core.common.ArithmeticOverflowException.
// allocateBasisPoints: units>=0, rate in 0..10_000.
// divideShare: nonnegative arguments, numerator<=denominator when denominator>0;
// zero denominator => ShareDivision(0, 0); exposes basisPointsFloor and remainder.

fun summarize(snapshot: FinancialSnapshot): SpendingSummary = calculateSpendingSummary(
    snapshot.operations, snapshot.categories, snapshot.period,
    snapshot.asset.identifier, snapshot.storeRevision, maxSlices = 6, // >=2
)
fun projectPlan(table: FinancialPlanningTable, snapshot: FinancialSnapshot): PlanningTableView =
    calculatePlanningTableView(table, snapshot.operations, snapshot.categories, snapshot.period)
// Use a period matching table.month and operations for table.assetIdentifier.
// Calculators filter by asset/period, use checked arithmetic, and do not validate
// imported documents as gateway mutations do. Supply valid domain records.
// financialMonthFor(operation, reportingTimeZone) / financialMonthFor(instant, reportingTimeZone): YearMonth
// calendar.reportingPeriod(month, timeZoneIdentifier): ReportingPeriod
// ReportingPeriod: month, timeZoneIdentifier, startInclusive: Instant, endExclusive: Instant.
```

Source: [gateway contract](src/commonMain/kotlin/org/orev/nahidka/feature/financial/gateway/FinancialGateway.kt), [mutation rules](src/commonMain/kotlin/org/orev/nahidka/feature/financial/store/FinancialReducer.kt), [calculations](src/commonMain/kotlin/org/orev/nahidka/feature/financial/calculation/FinancialSummaryCalculator.kt). Host integration: [FinancialSessionGraph](../../../src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionGraph.kt), [session owner](../../../src/commonMain/kotlin/org/orev/nahidka/di/FinancialSessionOwner.kt). Usage/tests: [FinancialHistoryViewModel](../../../ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/history/FinancialHistoryViewModel.kt), [FinancialPlanningViewModel](../../../ui/financial-management/src/commonMain/kotlin/org/orev/nahidka/ui/financialmanagement/planning/FinancialPlanningViewModel.kt), [view-model tests](../../../ui/financial-management/src/jvmTest/kotlin/org/orev/nahidka/ui/financialmanagement).
