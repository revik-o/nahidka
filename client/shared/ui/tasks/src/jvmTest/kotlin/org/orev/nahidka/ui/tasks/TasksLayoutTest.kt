package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.tasks.mock.TasksMockData
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TasksLayoutTest : TasksTest() {

    @Test
    fun expandedLayoutsAndDialogsRenderWithDemoData() = runSkikoComposeUiTest(size = Size(1200f, 800f)) {
        TasksMockData.seed(tasksScreenGraph.tasksManager)
        val darkTheme = mutableStateOf(false)
        setContent {
            NahidkaTheme(darkTheme.value) {
                Surface {
                    TasksScreen(
                        tasksViewModel,
                        Modifier
                            .size(1200.dp, 800.dp)
                            .testTag("tasks-preview"),
                    )
                }
            }
        }
        onNodeWithText("To Do (3)")
            .assertIsDisplayed()
        onNodeWithText("In Progress (1)")
            .assertIsDisplayed()
        onNodeWithText("Done (2)")
            .assertIsDisplayed()
        onNodeWithText("🤩")
            .assertIsDisplayed()
        saveScreen("desktop-board-light")
        darkTheme.value = true
        saveScreen("desktop-board-dark")
        darkTheme.value = false
        val dragDistance = onNodeWithText("Done (2)")
            .fetchSemanticsNode()
            .boundsInRoot
            .center - onNodeWithText("Plan the week")
            .fetchSemanticsNode()
            .boundsInRoot
            .center
        onNodeWithText("Plan the week")
            .performMouseInput {
                moveTo(center)
                press()
                moveBy(dragDistance)
            }
        saveScreen("desktop-drag")
        onNodeWithTag("tasks-preview")
            .performMouseInput { release() }
        onNodeWithText("List")
            .performClick()
        onNodeWithText("Organize the workspace")
            .assertIsDisplayed()
        saveScreen("desktop-list")
        onNodeWithText("Organize the workspace")
            .performClick()
        onNodeWithText("Edit task")
            .assertIsDisplayed()
        saveDialog("desktop-editor")
        onNodeWithText("Cancel")
            .performClick()
        onAllNodesWithContentDescription("More actions")
            .onFirst()
            .performClick()
        onNodeWithText("Rating reactions")
            .performClick()
        onNodeWithText("Reaction 4")
            .assertIsDisplayed()
        saveDialog("desktop-reactions")
    }

    @Test
    fun ukrainianCompactLayoutsFitAt360Dp() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("uk"))
        try {
            runSkikoComposeUiTest(size = Size(360f, 800f)) {
                TasksMockData.seed(tasksScreenGraph.tasksManager)
                setContent {
                    NahidkaTheme {
                        Surface {
                            TasksScreen(
                                tasksViewModel,
                                Modifier
                                    .size(360.dp, 800.dp)
                                    .testTag("tasks-preview"),
                            )
                        }
                    }
                }
                onNodeWithContentDescription("Створити завдання")
                    .assertIsDisplayed()
                saveScreen("phone-board-uk")
                onNodeWithText("Список")
                    .performClick()
                onNodeWithText("Plan the week")
                    .assertIsDisplayed()
                saveScreen("phone-list-uk")
                onNodeWithText("Plan the week")
                    .performClick()
                onNodeWithText("Редагування завдання")
                    .assertIsDisplayed()
                onNodeWithText("Зберегти")
                    .assertIsDisplayed()
                saveDialog("phone-editor-uk")
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private fun ComposeUiTest.saveScreen(name: String) {
        onNodeWithTag("tasks-preview")
            .saveImage(name)
    }

    private fun ComposeUiTest.saveDialog(name: String) {
        onNode(isDialog())
            .saveImage(name)
    }

    private fun SemanticsNodeInteraction.saveImage(name: String) {
        val bitmap = captureToImage()
        val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
        val encodedImage = checkNotNull(image.encodeToData())
        val outputDirectory = File("/tmp/nahidka-tasks-previews")
        assertTrue(outputDirectory.isDirectory || outputDirectory.mkdirs())
        File(outputDirectory, "$name.png").writeBytes(encodedImage.bytes)
        encodedImage.close()
        image.close()
    }
}
