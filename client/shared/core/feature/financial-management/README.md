# Financial management

`FinancialGateway` exposes operation CRUD, category management, monthly planning,
subscriptions, and coherent monthly snapshots. `FinancialModule.financialGateway`
is the session-scoped facade used by the UI. The in-memory adapter keeps records
for one session; settings use their separate platform persistence adapters.

## Exact amounts and identifiers

`Money(assetIdentifier, units)` stores exact signed integer minor units. Parsing
and formatting share one decimal conversion function and support the full Long
range. Percentages use exact basis-point division with an overflow-safe fallback.
Identifiers use full names throughout the API (`identifier`, `assetIdentifier`,
`categoryIdentifier`, `commandIdentifier`). Clocks, identifier generators, nullable
patches, and revision counters are exported by `:shared:core:common`.

## Commands and edits

Use `addOperation(AddFinancialOperation(...))`, `updateOperation(...)`, and
`removeOperation(...)`. Commands return `MutationResult.Committed` or `Rejected`.
Update/remove commands supply the current entity version. New entity versions are
based on the session revision so deleting and recreating an identifier does not
allow stale edits to affect its replacement. Active identifiers must be unique;
there are no permanent sets of deleted identifiers.

Operation kind is set at creation and cannot be patched. Nullable fields use
`NullablePatch.Keep`, `Set`, or `Clear`; omitted scalar fields are unchanged.
The editor submits only changed fields, stores occurrence time as an Instant, and
uses date/time controls in the configured reporting zone.

## Planning and arithmetic validation

`savePlanningTable` accepts a `FinancialPlanningTableInput`. Its rows reuse the
immutable domain `PlanningRow` model. Stored `FinancialPlanningTable` documents
own their fields directly and have no dependency on command types.
`PlanningTableView` contains a document, actual row totals, and overall totals.
Store revisions belong to event/snapshot envelopes, allowing direct structural
comparison of planning views.

Monthly operation groups are built once for planning rows. Mutation validation
checks aggregate arithmetic limits without building chart slices or row views.
The same checked arithmetic prevents amounts or planning totals from overflowing.

## Observation and lifecycle

`subscribeOperations(OperationQuery())`, `subscribeCategories(CategoryQuery())`,
and `subscribePlanning(PlanningQuery())` configure snapshot/insert/update/delete
handlers. Configure `onSnapshot` before `launchIn(ownerScope)`. Financial observers
use an isolated supervisor, bounded journal replay, and complete resync snapshots
when they fall behind. Frame publication barriers release in finally; an unreleased
frame fails visibly after five seconds. Cancel collectors with their owner and
call `close()` when the session ends. `observeFinancialSnapshot(MonthlyQuery(...))`
returns a coherent flow for summaries and planning.

Regression coverage includes exact basis-point arithmetic against BigInteger,
identifier reuse and stale edits, projection equality, release timeouts, overflow
rejection, draft validation, conflict retries, unchanged patches, immutable kinds,
and unsupported asset edits.
