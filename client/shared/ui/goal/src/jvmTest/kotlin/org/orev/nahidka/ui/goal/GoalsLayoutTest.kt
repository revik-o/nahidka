package org.orev.nahidka.ui.goal

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.goal.mock.GoalsMockData
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

private const val PHOTO_GOAL_TITLE = "Climb Hoverla at sunrise"
private const val SAMPLE_PHOTO_SIZE = 1024

@OptIn(ExperimentalTestApi::class)
class GoalsLayoutTest : GoalsTest() {

    @Test
    fun expandedLayoutsAndEditorRenderWithDemoData() = runSkikoComposeUiTest(size = Size(1200f, 800f)) {
        seedGoalsWithPhoto()
        val darkTheme = mutableStateOf(false)
        setContent {
            NahidkaTheme(darkTheme.value) {
                Surface {
                    GoalsScreen(
                        goalsViewModel,
                        Modifier
                            .size(1200.dp, 800.dp)
                            .testTag("goals-preview"),
                    )
                }
            }
        }
        awaitPhoto()
        onNodeWithText("Completed on 100%")
            .assertIsDisplayed()
        saveScreen("desktop-goals-light")
        darkTheme.value = true
        saveScreen("desktop-goals-dark")
        darkTheme.value = false
        onNodeWithText("Run a half marathon")
            .performClick()
        onNodeWithText("Edit goal")
            .assertIsDisplayed()
        saveDialog("desktop-editor")
    }

    @Test
    fun ukrainianCompactLayoutsFitAt360Dp() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("uk"))
        try {
            runSkikoComposeUiTest(size = Size(360f, 800f)) {
                seedGoalsWithPhoto()
                setContent {
                    NahidkaTheme {
                        Surface {
                            GoalsScreen(
                                goalsViewModel,
                                Modifier
                                    .size(360.dp, 800.dp)
                                    .testTag("goals-preview"),
                            )
                        }
                    }
                }
                awaitPhoto()
                onNodeWithContentDescription("Створити ціль")
                    .assertIsDisplayed()
                onNodeWithText("Виконано на 40%")
                    .assertIsDisplayed()
                saveScreen("phone-goals-uk")
                onNodeWithText("Run a half marathon")
                    .performClick()
                onNodeWithText("Редагування цілі")
                    .assertIsDisplayed()
                onNodeWithText("Зберегти")
                    .assertIsDisplayed()
                saveDialog("phone-editor-uk")
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private suspend fun seedGoalsWithPhoto() {
        goalsScreenGraph.goalsManager.createGoal(
            GoalCreationRequest(
                identifier = "photo-goal",
                title = PHOTO_GOAL_TITLE,
                description = "Reach the highest peak of Ukraine before the first light.",
                picture = GoalPicture.Photo(samplePhotoContent()),
                progressPercentage = 60f,
            ),
        )
        GoalsMockData.seed(goalsScreenGraph.goalsManager)
    }

    private fun samplePhotoContent(): ByteArray {
        val photoSize = SAMPLE_PHOTO_SIZE.toFloat()
        val photoBitmap = ImageBitmap(SAMPLE_PHOTO_SIZE, SAMPLE_PHOTO_SIZE)

        Canvas(photoBitmap).drawRect(
            rect = Rect(0f, 0f, photoSize, photoSize),
            paint = Paint().apply {
                shader = LinearGradientShader(
                    from = Offset.Zero,
                    to = Offset(0f, photoSize),
                    colors = listOf(Color(0xFFF6B26B), Color(0xFF7B42D0), Color(0xFF151523)),
                )
            },
        )

        return photoBitmap.encodeToPng()
    }

    private fun ComposeUiTest.awaitPhoto() {
        waitUntil {
            onAllNodesWithContentDescription(PHOTO_GOAL_TITLE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun ComposeUiTest.saveScreen(name: String) {
        onNodeWithTag("goals-preview")
            .saveImage(name)
    }

    private fun ComposeUiTest.saveDialog(name: String) {
        onNode(isDialog())
            .saveImage(name)
    }

    private fun SemanticsNodeInteraction.saveImage(name: String) {
        val outputDirectory = File("/tmp/nahidka-goals-previews")
        assertTrue(outputDirectory.isDirectory || outputDirectory.mkdirs())
        File(outputDirectory, "$name.png").writeBytes(captureToImage().encodeToPng())
    }

    private fun ImageBitmap.encodeToPng(): ByteArray {
        val image = Image.makeFromBitmap(asSkiaBitmap())
        val encodedImage = checkNotNull(image.encodeToData())
        val pngContent = encodedImage.bytes
        encodedImage.close()
        image.close()
        return pngContent
    }
}
