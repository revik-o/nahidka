# SocialBattery feature

The context exposes `batteryState` as a read-only `StateFlow` of immutable snapshots.
Collect it in the owner's lifecycle scope, or use Compose `collectAsState`.
Slow observers receive the latest complete snapshot; there are no event queues,
overflow disconnections, callback builders, or custom subscription jobs.
Cancel the collector with its owning lifecycle. Handle collector-specific failures
in the caller's supervision policy, as with any ordinary Kotlin flow.

Snapshots have structural equality and carry a revision. Changing writes increment
the revision once; no-op writes keep it. Settings persist through the required
`SettingsLocalDataSource` before publishing. Task batches validate completely before
committing and use persistent maps; invalid batches leave state unchanged.

`SocialBatteryManager.updateBattery` updates the single optional battery value.
A percentage must be between 0 and 100. Mutation results expose `changed`.
