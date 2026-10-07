# Goals UI

`GoalsScreen` lists goal cards: a picture (an emoji, a photo from the device, or the title's first letter), the title, “Completed on N%”, the description (up to three lines), a progress bar and the optional deadline. Clicking a card opens the editor; the card's ⋮ menu has Edit and Delete (with confirmation). The editor holds the title, description, picture (12 emoji or **Choose photo**), a 0–100 % progress slider in 5 % steps and an optional deadline. Layouts use the screen width with the shared 600 dp breakpoint. Strings are available in English, Ukrainian, and Russian.

Photos are picked by `rememberPhotoPicker` from `shared/ui/common`: the Android Photo Picker, a desktop file dialog, a browser file input, or the iOS `PHPickerViewController`. The core keeps the original bytes; cards decode them off the main thread and scale them to at most 512 px.

`GoalsScreenGraph` uses the same `GoalsBindings` as `GoalsSessionGraph`. The app retains the ViewModel in its session and exposes a Goals entry.

## Temporary data

All production demo records live in [GoalsMockData.kt](src/commonMain/kotlin/org/orev/nahidka/ui/goal/mock/GoalsMockData.kt). `App.kt` invokes its seed function once for each remembered goals graph: five goals covering emoji and letter pictures, descriptions, optional deadlines, and a completed goal. Changes last for the app session; restarting creates fresh demo data.

To remove the demo seed, delete `GoalsMockData.kt` and remove its import and `LaunchedEffect` call from `App.kt`. Persistence and the backend remain outside this implementation.

## Verification

```sh
./gradlew :shared:core:feature:goals:jvmTest --rerun :shared:ui:common:jvmTest --rerun :shared:ui:goal:jvmTest --rerun
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :webApp:compileKotlinJs :webApp:compileKotlinWasmJs
./gradlew -Pkotlin.native.enableKlibsCrossCompilation=true :shared:ui:common:compileKotlinIosArm64 :shared:ui:goal:compileKotlinIosArm64
```

`GoalsLayoutTest` renders desktop light and dark lists, the editor, and Ukrainian phone layouts at a true 360 dp viewport, including a photo goal. PNG previews are written to `/tmp/nahidka-goals-previews`.

The photo pickers open native system UI, so tests cover photos at the ViewModel and rendering level, not the picker dialogs themselves. iOS compiles on Linux through klib cross-compilation but was not run.
