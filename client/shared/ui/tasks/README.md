# Tasks UI

`TasksScreen` provides an adaptive board and list, task creation and editing, optional due dates, deletion confirmation, customizable reactions, mouse dragging, touch long-press dragging, and menu-based status changes. Layouts use the screen width with a 600 dp breakpoint. Strings are available in English, Ukrainian, and Russian.

The core owns the tasks and reaction levels in one atomic state. Only Done tasks can be rated. Leaving Done clears the rating; deleting a reaction clears affected task ratings. `TasksScreenGraph` uses the same `TasksBindings` as `TasksSessionGraph`. The app retains the ViewModel in its session and exposes a Tasks entry.

## Temporary data

All production demo records live in [TasksMockData.kt](src/commonMain/kotlin/org/orev/nahidka/ui/tasks/mock/TasksMockData.kt). `App.kt` invokes its seed function once for each remembered tasks graph. There are three To Do tasks, one In Progress task, and two Done tasks, covering descriptions, optional dates, and ratings. Changes last for the app session; restarting creates fresh demo data.

To remove the demo seed, delete `TasksMockData.kt` and remove its import and `LaunchedEffect` call from `App.kt`. The core and ViewModel contain no demo records. Persistence and the backend remain outside this implementation.

## Verification

```sh
./gradlew :shared:core:feature:tasks:jvmTest --rerun :shared:ui:common:jvmTest --rerun :shared:ui:tasks:jvmTest --rerun
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :webApp:compileKotlinJs :webApp:compileKotlinWasmJs
```

The tasks tests cover actual mouse and touch input, card editing, compact movement menus, reaction selection and removal, creation, deletion confirmation, reaction customization, date selection and clearing, and rejected saves. Core tests cover rating rules, atomic rejection, immutable snapshots, task order, and both documented examples. `TasksHostTest` exercises the actual app entry and preservation of task edits across navigation.

`TasksLayoutTest` renders desktop light and dark boards, a drag in progress, the list, the editor, the reactions dialog, and Ukrainian phone layouts at a true 360 dp viewport. PNG previews are written to `/tmp/nahidka-tasks-previews`.

iOS requires a macOS build host and was not verified here. Dragging changes status without changing task order and does not auto-scroll columns. Touch moves on compact screens use the card menu. No backend or persistence is required.
