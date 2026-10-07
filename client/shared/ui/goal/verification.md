# Goals implementation verification

Verified on 2026-10-07 against the current worktree. All ten implementation steps in [plan.md](plan.md) are complete. No Python was used.

| Requirement | Implementation and evidence |
|---|---|
| Step 1: core fields, validation and DI | Description, emoji/photo picture, date-only deadline, nullable patches, shared progress range, `GoalsBindings` and `IdentifierGeneratorBindings` are applied. All four `GoalsContextTest` tests pass. The README creation/update/deletion example was extracted and executed as a temporary test in an isolated copy; it passes. |
| Step 2: shared UI and platform photo support | Shared fields, chips, card list, date text, editing actions, mutation/deletion dialogs and public translated resources are applied. Three existing dialog tests and two new mutation tests pass. Android, JVM, web and iOS photo-picker implementations compile. Photo decoding and downscaling execute during layout tests. |
| Step 3: Tasks migration | Tasks uses the shared components, dialogs, resources, padding and identifier bindings. The replaced Tasks components are deleted. Ten Tasks core tests and twenty Tasks UI tests pass, including mouse/touch dragging and chip deselection. |
| Step 4: Goals build, model, ViewModel and graph | Production graph, session-scoped context, direct repository state flow and draft/request conversion are applied. Four ViewModel tests pass, covering creation, blank title, photo replacement/progress and deletion. |
| Step 5: localization | English, Ukrainian and Russian resources match the planned strings. Shared action and field strings are imported from `ui/common`. Ukrainian renders use the actual localized resources. |
| Step 6: pictures and cards | Twelve emoji choices, title-letter fallback, cropped photos, responsive picture sizes, bounded title/description text, progress percentage/bar, completion color, optional date and editing actions are applied. Screen and rendered-layout tests exercise the cards. |
| Step 7: editor and deletion | Scrolling editor includes title, description, picture selection/removal, 5% progress steps and optional deadline. Mutation rejections stay in the dialog. Screen tests cover save validation, progress, emoji removal and deletion confirmation/cancellation. |
| Step 8: screen, demo data and documentation | Width-based compact/expanded screen, creation controls, empty state, card list and dialogs are applied. The five planned demo records seed once per remembered graph. The module README is present; `GoalsTable` is deleted. |
| Step 9: tests and rendered layouts | All planned test files are present. Desktop light/dark lists, desktop editor, 360 dp Ukrainian list and Ukrainian editor were freshly rendered and visually inspected, including the photo goal. |
| Step 10: host | Goals has its own entry and a session-retained ViewModel. `GoalsHostTest` verifies demo data, editing and retention across navigation. `TasksHostTest` passes. With the concurrent social-battery implementation, the obsolete personal-feature enum/screen and branches are removed as specified in §5. |

An additional source audit verified all 90 planned source blocks and deletions outside the merged host. The host integration was inspected separately and tested through the real `App()`.

## Regression runs

The following command passed on three consecutive runs. Every test task executed; none used cached test results.

```sh
JAVA_HOME=/home/oleg/.gradle/jdks/jetbrains_s_r_o_-25-amd64-linux.2 ./gradlew \
  :shared:core:feature:goals:jvmTest --rerun \
  :shared:core:feature:tasks:jvmTest --rerun \
  :shared:ui:common:jvmTest --rerun \
  :shared:ui:goal:jvmTest --rerun \
  :shared:ui:tasks:jvmTest --rerun \
  :shared:jvmTest --rerun \
  --max-workers=2 --offline --console=plain
```

All 52 tests specified by the plan pass. The merged host adds one social-battery test, giving 53 tests with zero failures, errors or skips. Logs are `/tmp/nahidka-goals-final-tests-first.log`, `/tmp/nahidka-goals-final-tests-2.log` and `/tmp/nahidka-goals-final-tests-1.log` (the third run).

An earlier overlapping build produced EOF errors while both builds wrote shared test-result files. The three final runs above completed successfully after that overlap ended.

## Negative checks and documentation example

In `/tmp/nahidka-goals-audit`, disabling chip deselection and dropping the draft's picture patch caused exactly the planned tests to fail:

- `TasksScreenTest.choosingSelectedReactionAgainRemovesRating`
- `GoalsScreenTest.choosingSelectedEmojiAgainRemovesPicture`
- `GoalsViewModelTest.goalEditingReplacesPictureWithPhotoAndUpdatesProgress`

The isolated copy's modified source files were restored afterward. The real worktree was never changed for these negative checks. Evidence is in `/tmp/nahidka-goals-negative-audit.log` and that copy's JUnit XML reports. The extracted README example passed separately; its log is `/tmp/nahidka-goals-documentation-audit.log`.

## Platform builds and visual checks

`/tmp/nahidka-goals-platforms.log` records a successful build of:

- `:androidApp:assembleDebug`
- `:desktopApp:compileKotlin`
- `:webApp:compileKotlinJs` and `:webApp:compileKotlinWasmJs`
- `compileKotlinIosArm64` and `compileKotlinIosSimulatorArm64` for core common, core goals, UI common, UI tasks and UI goal, using `-Pkotlin.native.enableKlibsCrossCompilation=true`

The host builds compile the changed modules for Android, JVM, JS and Wasm. JVM test sources compile as part of the regression command. No new Kotlin compiler warnings appeared; the existing disabled-iOS-test notice and JVM native-access/Gradle deprecation notices remain. `GoalsManager` uses class-level `@Inject`. `git diff --check` passes.

The five fresh PNG renders in `/tmp/nahidka-goals-previews` were inspected: `desktop-goals-light.png`, `desktop-goals-dark.png`, `desktop-editor.png`, `phone-goals-uk.png` and `phone-editor-uk.png`. Visible editor controls fit; Ukrainian labels wrap and the compact list scrolls.

## Runtime limits retained from the plan

Native photo-picker dialogs were compiled but not driven through their system UI. iOS was cross-compiled to klibs on Linux, without linking or running on macOS. Touch input was synthetic. These are the plan's explicitly unverified runtime checks.

Goals and original photo bytes remain in memory for the session. Displayed photos are scaled to at most 512 px. Persistence, stored-photo encoding, the app side menu, dashboard integration, sorting/filtering and overdue highlighting remain outside this plan. Desktop/web HEIC decoding and the Windows native file-filter limitation are unchanged.
