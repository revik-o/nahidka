# Tasks implementation audit

Completed on 2026-10-05. All 78 file artifacts named in `plan.md` exist. The implementation follows all eleven steps, with additional tests, centralized task demo data, and the small host dependency repairs needed to run the actual app.

| Plan step | Current implementation and evidence |
|---|---|
| 1. Core | Local task DTOs, optional date patches, reaction levels, atomic replacement, and shared DI bindings. Eight `TasksContextTest` cases verify rating rules, date changes, ordering, snapshot immutability, and rejected atomic batches. Two `TasksDocumentationTest` cases execute both README examples. |
| 2. Shared UI | Registered `shared/ui/common` module with all planned layout, selector, menu, date, table, format, and dialog artifacts. Three `DialogControllerTest` cases verify successful, rejected, and invalid submissions. |
| 3. Models, ViewModel, DI | All planned models and controllers exist. `TasksScreenGraph` includes the core `TasksBindings`; seven `TasksViewModelTest` cases use that graph and verify creation, validation, editing, movement, deletion, reaction addition, and reaction deletion. |
| 4. Resources | Both new UI modules have matching English, Ukrainian, and Russian resource keys: 31 tasks strings and 4 common strings in each language. The Ukrainian compact layout is rendered at 360 dp. |
| 5. Shared task components | Status labels and badges, move/edit/delete actions, reaction toggles, and date text are used by board and list views. Interaction tests exercise their real controls. |
| 6. Board | Three counted columns with empty states; compact scrollable status tabs; mouse and touch long-press dragging; fading source, following card, and highlighted target. Both real drag tests pass and fail when dropping is disabled. The drag preview was inspected. |
| 7. List | Weighted desktop columns with a sticky header and compact wrapping rows, ellipsized descriptions, status menus, optional dates, Done-only reactions, and edit/delete actions. Desktop and compact previews were inspected; list reaction input is tested. |
| 8. Dialogs | Scrollable creation/editing, deletion confirmation, and reaction customization dialogs. UI tests verify creation, cancellation and confirmation of deletion, date picker selection and clearing, reaction editing and reordering, and a rejected save that keeps the dialog open. ViewModel tests additionally verify reaction addition and deletion. |
| 9. Screen | Width measured using `BoxWithConstraints`; shared 600 dp breakpoint; adaptive header; Board/List stored with `rememberSaveable`; snackbar collection for rejected quick changes; all three dialogs connected. Light/dark desktop and Ukrainian phone renders passed. |
| 10. Tests | All original 21 planned tests are present, plus twelve additional module tests. All 33 module tests passed on three consecutive forced runs. |
| 11. Host | Actual app Tasks entry, remembered session graph, session-owned ViewModel, old personal tasks tab removed. `TasksHostTest` navigates into Tasks, edits a demo task, leaves and returns, and verifies the edit and count persist. The app builds for desktop, Android, JS, and Wasm. |

All production task fixtures are in `mock/TasksMockData.kt`; `App.kt` has one seed call per remembered graph. There is no task fixture in the core or ViewModel. Removing the file, its import, and the seed effect removes demo initialization. Data stays in memory for the session, as permitted by the plan and the backend constraint. No Python scripts were used.

Host repairs restore the missing theme, state holder, settings screen, goals table, social battery widget, and local goal record needed by existing host code. The host no longer depends on the unused dashboard core service that imports the deleted backend API. Existing financial work was preserved.

## Verification records

- `/tmp/nahidka-tasks-final-1.log`, `-2.log`, and `-3.log`: three consecutive successful fresh module test runs, using `--rerun` for each test task.
- `/tmp/nahidka-tasks-negative.log`: disabling the drop callback makes both mouse/touch drag tests fail; disabling the reaction toggle makes its removal test fail. All three failed as expected. Both source files were restored and compared with their originals before the three positive runs.
- `/tmp/nahidka-tasks-host.log`: successful `:shared:jvmTest`, `:desktopApp:compileKotlin`, `:androidApp:assembleDebug`, `:webApp:compileKotlinJs`, and `:webApp:compileKotlinWasmJs` checks.
- `/tmp/nahidka-tasks-web-test-compilation.log`: successful JS and Wasm test-source compilation for tasks core, shared UI, and tasks UI. Browser test execution was not part of this check.
- XML test results under each module's `build/test-results/jvmTest`: 33 module cases and one host case, with no failures or skipped cases.
- `/tmp/nahidka-tasks-previews`: nine inspected PNGs covering desktop light/dark boards, active dragging, list, editor, reaction editor, and Ukrainian phone board/list/editor at a true 360 dp viewport.
- `git diff --check`: passed.

The plan's existing limits remain: no persistence or backend, no sorting/filtering or overdue highlight, no drag auto-scroll or position changes, and no side-menu redesign. iOS requires macOS and remains unverified on this Linux host. A stationary touch long press can also open the editor, as documented in the original plan. These are the plan's stated limits, rather than missing implementation steps.
