# Financial management

This Kotlin Multiplatform module provides an exact-money posted ledger, monthly planning projections, category management, and a snapshot-plus-change subscription API. Its source targets Android, JVM, iOS, JavaScript, and Wasm. The current `InMemoryFinancialGateway` is a development adapter: it loses records when the process or session ends and does not synchronize across devices. Do not use it for production records.

Add the feature module and its public API dependencies to a client module:

```kotlin
commonMain.dependencies {
    implementation(project(":shared:core:feature:financial-management"))
}
```

The feature exports coroutines, immutable collections, and `kotlinx-datetime` through its API configuration. Android consumers that support API 24 should enable core library desugaring in the Android application module. Browser targets include the `@js-joda/timezone` package and its module initializer so IANA reporting zones are available.

## Create one session graph

Create the runtime configuration and graph once for the active session. The application's `FinancialSessionOwner` keeps the graph and `ViewModelStore` stable across recomposition and navigation, then clears the store and closes the gateway when the session ends.

```kotlin
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.FinancialSessionConfig

val sessionConfig = FinancialSessionConfig(
    sessionIdentity = sessionIdentity,
    workspaceIdentity = workspaceIdentity,
    assets = persistentListOf(AssetDefinition("iso4217:USD", "USD", 2)),
    defaultAssetId = "iso4217:USD",
    reportingTimeZone = "Europe/Kyiv",
)
val graph = createGraphFactory<FinancialSessionGraph.Factory>().create(sessionConfig)
val finance = graph.finance
val financialContext = finance.financialContext
val financialManager = finance.financialManager
val financialPlanningContext = finance.financialPlanningContext
val financialPlanningManager = finance.financialPlanningManager
val financialCategoryManager = finance.financialCategoryManager
```

The session owner binds `FinancialClock` to `SystemFinancialClock`, `FinancialIdGenerator` to `RandomFinancialIdGenerator`, `ExactMoneyFormatter`, and `FinancialErrorReporter` to `NoOpFinancialErrorReporter`. Replace the no-op reporter with application logging when needed. Application code supplies `FinancialSessionConfig` when it creates the graph.

Core-only consumers and tests can bind an externally created fake or replacement gateway without importing the application's shared or UI modules. This declaration lives in `FinancialApiExampleGraph.kt`:

```kotlin
package org.orev.nahidka.feature.financial

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import org.orev.nahidka.feature.financial.di.FinancialModule
import org.orev.nahidka.feature.financial.di.FinancialSessionScope
import org.orev.nahidka.feature.financial.gateway.FinancialGateway

@DependencyGraph(FinancialSessionScope::class)
interface FinancialApiExampleGraph {
    val finance: FinancialModule

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides gateway: FinancialGateway): FinancialApiExampleGraph
    }
}
```

Use its generated factory to retrieve the constructor-injected facade:

```kotlin
import dev.zacsweers.metro.createGraphFactory

val exampleGraph = createGraphFactory<FinancialApiExampleGraph.Factory>().create(fakeGateway)
val exampleFinance = exampleGraph.finance
```

The fake gateway, `FinancialSessionConfig`, IDs, queries, clock, and display functions are application or test dependencies. The graph owns manager/context wiring; do not construct a second gateway for one session or manually assemble those services.

## Exact money, operations, and reporting periods

`Money` stores signed integer units with a stable asset ID. Operations store a positive amount and an explicit kind (`INCOME`, `EXPENSE`, or `REFUND`); the kind determines how it affects totals. For a two-decimal USD asset, 1,234 units means 12.34 USD. Parse decimal text with `parseMoneyText` and format it through `ExactMoneyFormatter` or `formatMoneyInput`; never parse a formatted display string back into a command.

```kotlin
import org.orev.nahidka.feature.financial.calculation.parseMoneyText
import org.orev.nahidka.feature.financial.dto.AssetDefinition
import org.orev.nahidka.feature.financial.dto.MonthlyQuery
import org.orev.nahidka.feature.financial.support.ExactMoneyFormatter

val usd = AssetDefinition("iso4217:USD", "USD", 2)
val amount = parseMoneyText("1,234.56", usd) ?: error("Invalid amount")
val display = ExactMoneyFormatter().format(amount, usd)
val selectedMonth = YearMonth(2026, 10)
val query = MonthlyQuery(selectedMonth, usd.id)
```

Asset IDs and fraction digits come from the immutable session registry. Operations use one asset each; arithmetic and projections never combine different assets. The reporting time zone is explicit in `FinancialSessionConfig`. `FinancialCalendar` makes each monthly interval inclusive at its start and exclusive at the next month's start, including daylight-saving changes.

`PaymentMethod.CASH`, `CARD`, and `CRYPTOCURRENCY` record the method used for that posted operation. They do not represent bank balances, card liabilities, transfers, crypto quantity, or a fiat conversion. Mixed assets stay in separate summaries.

## Read the latest state and subscribe

Use `observeFinancialSnapshot` when a screen needs one coherent monthly view. Its operations, categories, spending summary, planning result, and revision all come from one immutable store frame. For incremental operation rendering, start with a snapshot handler, then add the change handlers you use:

```kotlin
val job = financialContext.subscribe(operationQuery)
    .onSnapshot { renderAll(it.entities) }
    .onUpdate { renderUpdated(it.after) }
    .onDelete { renderDeleted(it.before.id) }
    .onInsert { renderInserted(it.after) }
    .onResync { renderAll(it.entities) }
    .onError { showFailure(it) }
    .launchIn(screenScope)
```

`launchIn` requires a coroutine scope with a `Job`, normally the destination's lifecycle scope. A snapshot seeds the consumer. Each retained commit produces a batch; `onBatch` and individual handlers can both be configured, but consumers should normally use one style to avoid applying the same change twice. When the bounded journal no longer contains the required suffix, `onResync` replaces local state with a current complete snapshot. If omitted, the `onSnapshot` handler also handles resyncs.

Cancelling the returned job stops that subscription. A callback exception reports to `onError` and ends only that subscription; without `onError`, the subscription job fails and reports through the injected reporter. Cancellation is propagated and is never treated as an ordinary callback error. Restarting a subscription produces a new snapshot; external side effects do not have an exactly-once guarantee.

For charts and plan calculations, collect one monthly projection instead of combining several independently collected feeds:

```kotlin
import org.orev.nahidka.feature.financial.dto.MonthlyQuery

screenScope.launch {
    financialContext.observeFinancialSnapshot(MonthlyQuery(selectedMonth, selectedAssetId)).collect { snapshot ->
        renderOperations(snapshot.operations)
        renderSpending(snapshot.spending)
        renderPlanning(snapshot.planning)
    }
}
```

## Add, update, and remove operations

Commands contain stable IDs, exact amounts, explicit kinds and payment methods, and an ISO `Instant`. Refunds must reference an existing expense in the same workspace and asset; their category is inherited from that expense. A refund cannot exceed the unrefunded expense amount.

```kotlin
import org.orev.nahidka.feature.financial.command.AddFinancialOperation
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.NewFinancialOperation
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult
import org.orev.nahidka.feature.financial.dto.OperationKind
import org.orev.nahidka.feature.financial.dto.PaymentMethod

val addCommand = AddFinancialOperation(
    meta = CommandMeta(ids.next()),
    operation = NewFinancialOperation(
        id = ids.next(),
        amount = Money("iso4217:USD", 1_234),
        kind = OperationKind.EXPENSE,
        categoryId = groceriesCategory.id,
        paymentMethod = PaymentMethod.CARD,
        occurredAt = clock.now(),
        description = "Groceries",
    ),
)
val created = when (val result = financialManager.addNewFinancialManipulation(addCommand)) {
    is MutationResult.Committed -> result.value
    is MutationResult.Rejected -> {
        showCommandError(result.error)
        return
    }
}
```

An update supplies the current entity version. `NullablePatch.Keep` preserves a nullable field, `Set(value)` replaces it, and `Clear` removes it. Omitted non-null fields are also preserved.

```kotlin
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.FinancialOperationPatch
import org.orev.nahidka.feature.financial.command.NullablePatch
import org.orev.nahidka.feature.financial.command.RemoveFinancialOperation
import org.orev.nahidka.feature.financial.command.UpdateFinancialOperation
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.MutationResult

val updated = financialManager.updateFinancialManipulation(
    UpdateFinancialOperation(
        meta = CommandMeta(ids.next()),
        id = created.id,
        expectedVersion = created.version,
        patch = FinancialOperationPatch(
            amount = Money("iso4217:USD", 1_500),
            description = NullablePatch.Clear,
        ),
    ),
)
if (updated is MutationResult.Committed) {
    val removed = financialManager.removeFinancialManipulation(
        RemoveFinancialOperation(
            meta = CommandMeta(ids.next()),
            id = updated.value.id,
            expectedVersion = updated.value.version,
        ),
    )
    showDeleteResult(removed)
}
```

For a typed conflict, keep the user's draft and show the current entity. They can reload its fields or deliberately retry the reviewed draft against the latest version:

```kotlin
import org.orev.nahidka.feature.financial.dto.FinancialError
import org.orev.nahidka.feature.financial.dto.MutationResult

val draftDescription = editedDescription
when (val result = financialManager.updateFinancialManipulation(updateCommand)) {
    is MutationResult.Committed -> renderSaved(result.value)
    is MutationResult.Rejected -> when (val error = result.error) {
        is FinancialError.OperationConflict -> {
            keepDraft(draftDescription)
            showConflict(error.current)
        }
        else -> showCommandError(error)
    }
}
```

The `commandId` is a bounded retry key retained only within the live session. Reuse the original immutable command with the same ID after an uncertain result; the gateway returns its original receipt without another commit. A different payload under the same retained ID returns `FinancialError.CommandIdReused`.

```kotlin
val firstAttempt = financialManager.addNewFinancialManipulation(addCommand)
val retryAfterUncertainCompletion = financialManager.addNewFinancialManipulation(addCommand)
```

## Manage categories

Category IDs remain stable when names change. Names are trimmed and compared using Kotlin's lowercase conversion for duplicate checks; comparison does not perform Unicode normalization. Deleting an unreferenced category requires its current version. A referenced category cannot be deleted; archive it to keep historical operation and plan labels while preventing new references. Existing plan rows may retain an archived category.

```kotlin
import org.orev.nahidka.feature.financial.command.ArchiveFinancialCategory
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.CreateFinancialCategory
import org.orev.nahidka.feature.financial.command.UpdateFinancialCategory
import org.orev.nahidka.feature.financial.dto.MutationResult

val createdCategory = financialCategoryManager.createCategory(
    CreateFinancialCategory(CommandMeta(ids.next()), ids.next(), "Groceries", "cart"),
)
val renamedCategory = financialCategoryManager.updateCategory(
    UpdateFinancialCategory(
        CommandMeta(ids.next()),
        groceriesCategory.id,
        groceriesCategory.version,
        name = "Food and groceries",
    ),
)
if (renamedCategory is MutationResult.Committed) {
    val archived = financialCategoryManager.archiveCategory(
        ArchiveFinancialCategory(CommandMeta(ids.next()), renamedCategory.value.id, renamedCategory.value.version),
    )
    showCategoryResult(archived)
}
```

Choose an active category for a new expense. An existing expense may keep its archived category during an edit; a refund inherits its original expense's category.

## Save and observe a monthly plan

Planning input is saved as one versioned document. A null expected version requests creation; an edit must send the current version. Each row stores a stable ID, category, planned amount, inclusion flag, and optional preferred payment method. Actual spending and derived totals are projected outputs, not submitted values.

```kotlin
import org.orev.nahidka.feature.financial.command.CommandMeta
import org.orev.nahidka.feature.financial.command.FinancialPlanningTableInput
import org.orev.nahidka.feature.financial.command.PlanningRowInput
import org.orev.nahidka.feature.financial.command.SavePlanningTable
import org.orev.nahidka.feature.financial.dto.Money
import org.orev.nahidka.feature.financial.dto.PaymentMethod
import org.orev.nahidka.feature.financial.dto.SavingsGuidelinePolicy

val input = FinancialPlanningTableInput(
    id = ids.next(),
    month = selectedMonth,
    assetId = "iso4217:USD",
    openingAvailable = Money("iso4217:USD", 100_000),
    savingsPolicy = SavingsGuidelinePolicy(
        reserve = Money("iso4217:USD", 15_000),
        allocationBasisPoints = 5_000,
    ),
    rows = persistentListOf(
        PlanningRowInput(
            id = ids.next(),
            categoryId = groceriesCategory.id,
            plannedAmount = Money("iso4217:USD", 30_000),
            included = true,
            preferredPaymentMethod = PaymentMethod.CARD,
        ),
    ),
)
val saved = financialPlanningManager.saveTable(
    SavePlanningTable(CommandMeta(ids.next()), expectedVersion = null, table = input),
)
showSaveResult(saved)
```

Planning subscriptions publish a snapshot and versioned insert/update/delete batches. The current plan table view carries input and calculated actuals together:

```kotlin
import org.orev.nahidka.feature.financial.dto.PlanningQuery

val planJob = financialPlanningContext.subscribe(PlanningQuery(selectedMonth, selectedAssetId))
    .onSnapshot { renderPlans(it.entities) }
    .onUpdate { renderPlan(it.after) }
    .onDelete { removePlan(it.before.document.input.id) }
    .onInsert { renderPlan(it.after) }
    .onResync { renderPlans(it.entities) }
    .launchIn(screenScope)
```

Each planning row exposes category, monthly planned cost, whether it is included, gross expenses, refunds, net spent, remaining budget, overspend, preferred payment, and actual cash/card/cryptocurrency amounts. During the month, remaining budget is provisional category underspend; after the calendar month ends, it is still a computed underspend and is never an automatic savings transfer. Refunds are grouped in their occurrence month and can make a category's signed net spend negative.

## Monthly and donut summaries

`FinancialSnapshot.spending` exposes exact `Money` totals for gross expense, refunds, signed net expense, drawable positive category total, and refund credits. Slice percentages are integer basis points and sum to exactly 10,000 when drawable slices exist. The chart uses positive category net expense only; negative category net values appear as refund credits. When the slice limit is exceeded, the smaller positive categories are grouped into `Other`.

With opening available 100,000 units, income 50,000, and net posted expense 83,000, the current available amount is 67,000 units. If included categories reserve 12,000 more, projected available after planning is 55,000. A 15,000 reserve and 50% guideline produce a suggested savings amount of 20,000. Remaining category budget is already included in reserved funds; do not count it again as cash. Without an opening amount and plan, `PlanningNotConfigured` keeps funds and forecast unavailable rather than treating the opening amount as zero.

The savings guideline is optional user policy, not a financial recommendation. It is computed only when a reserve and allocation rate are configured. Current available preserves deficits; only the allocatable amount above the reserve is clamped to zero.

## Coroutine guarantees and session teardown

The in-memory reducer validates and commits each command atomically under a mutex. Successful changed commands advance one session-local revision; no-ops, failures, and retries do not add a commit. Expected versions protect edits, and successful command receipts are retained in a bounded configurable window. The initial subscription snapshot is captured before the watcher starts and its frame release fence is completed before delivery. Handlers run outside the store lock, so a handler may submit another command. Cancellation before a waiting command acquires the lock prevents that command from committing; cancellation after commit may prevent the caller from receiving the receipt, which is why an identical retry reuses the command ID.

The journal also has a configured capacity. If a consumer falls behind beyond retention, it receives a complete resync snapshot instead of an incomplete suffix. Session close rejects later commands, clears sensitive in-memory data, terminates subscriptions, and prevents later reads from returning a stale open frame. Application owners should clear lifecycle ViewModels and close the gateway on logout or workspace switch:

```kotlin
viewModelStore.clear()
graph.gateway.close()
```

The application session owner performs this teardown when its active session is disposed. A new workspace or login receives a new session identity and gateway instance.

## Errors, adapter replacement, and executable examples

Commands return `MutationResult.Committed<T>` or `MutationResult.Rejected`. Rejections distinguish validation errors, missing entities, operation/category/planning conflicts, category-in-use results, reused command IDs, closed sessions, unavailable storage, and forbidden actions. They are values; callback failures and closed-session reads use coroutine exceptions and remain separate from command validation results.

`FinancialGateway` is the adapter boundary. A future durable or remote implementation must preserve atomic validation, version and idempotency semantics, coherent revisions, complete snapshots, replay/resync behavior, authorization, and close lifecycle. The current adapter provides no remote feed, durability, offline outbox, or cross-device synchronization.

`FinancialApiExamplesTest` compiles and exercises the public graph, commands, retry behavior, operation and planning observers, conflicts, categories, and teardown against the current API. Core ledger, planning, subscription, and JVM concurrency behavior are covered by the module's common and JVM tests.
