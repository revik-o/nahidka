package org.orev.nahidka

import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import org.orev.nahidka.shell.ApplicationTopBar
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PREVIEW_DIRECTORY = "/tmp/nahidka-shell-previews"

@OptIn(ExperimentalTestApi::class)
class ApplicationShellLayoutTest : ApplicationTest() {

    @Test
    fun desktopLayoutRendersDashboardNotificationsAndFinanceInBothThemes() {
        SettingsTheme.entries
            .filter { theme -> theme != SettingsTheme.FOLLOW_SYSTEM }
            .forEach { theme ->
                renderApplication(width = 1440, height = 900, theme = theme) { themeName ->
                    saveScreen("desktop-dashboard-$themeName")
                    onNodeWithContentDescription("Notifications")
                        .performClick()
                    onNodeWithText("Task “Plan the week” is overdue")
                        .assertIsDisplayed()
                    saveScreen("desktop-notifications-$themeName")
                    onNodeWithText("Task “Plan the week” is overdue")
                        .performClick()
                    onNodeWithText("Board")
                        .assertIsDisplayed()
                    saveScreen("desktop-tasks-$themeName")
                    openDestination("Finances")
                    onNode(hasText("Finances") and isSelectable())
                        .assertIsSelected()
                    onNodeWithText("Finance history")
                        .assertIsDisplayed()
                    saveScreen("desktop-finance-$themeName")
                }
            }
    }

    @Test
    fun phoneLayoutRendersFinanceAndKeepsAllNavigationButtonsAccessible() {
        listOf(320, 390).forEach { phoneWidth ->
            renderApplication(width = phoneWidth, height = 844, theme = SettingsTheme.DARK) { themeName ->
                saveScreen("phone-$phoneWidth-dashboard-$themeName")
                listOf("Home", "Battery", "Tasks", "Goals", "Finance", "Settings").forEach { navigationLabel ->
                    val navigationButton = onNode(hasText(navigationLabel) and isSelectable())
                        .assertIsDisplayed()
                    val navigationButtonBounds = navigationButton.getUnclippedBoundsInRoot()
                    assertTrue(navigationButtonBounds.right - navigationButtonBounds.left >= 48.dp)
                    assertTrue(navigationButtonBounds.bottom - navigationButtonBounds.top >= 48.dp)
                    val navigationLabelLayoutResults = mutableListOf<TextLayoutResult>()
                    onNode(hasText(navigationLabel) and hasAnyAncestor(isSelectable()), useUnmergedTree = true)
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getTextLayoutResults ->
                            getTextLayoutResults(navigationLabelLayoutResults)
                        }
                    assertTrue(navigationLabelLayoutResults.isNotEmpty())
                    navigationLabelLayoutResults.forEach { navigationLabelLayoutResult ->
                        assertTrue(!navigationLabelLayoutResult.isLineEllipsized(0), "$navigationLabel is truncated at $phoneWidth")
                        assertEquals(navigationLabel.length, navigationLabelLayoutResult.getLineEnd(0, visibleEnd = true))
                    }
                }
                openDestination("Battery")
                saveScreen("phone-$phoneWidth-social-battery-$themeName")
                openDestination("Goals")
                saveScreen("phone-$phoneWidth-goals-$themeName")
                openDestination("Finance")
                onNode(hasText("Finance") and isSelectable())
                    .assertIsSelected()
                onNodeWithText("Finance history")
                    .assertIsDisplayed()
                saveScreen("phone-$phoneWidth-finance-$themeName")
                onNodeWithContentDescription("Add financial operation")
                    .performClick()
                onNodeWithText("New financial operation")
                    .assertIsDisplayed()
                onNodeWithText("Cancel")
                    .performClick()
                onNodeWithText("Finance planning")
                    .performClick()
                onNodeWithText("Amount of money: 3000.00 USD")
                    .assertIsDisplayed()
                saveScreen("phone-$phoneWidth-finance-planning-$themeName")
                onNodeWithContentDescription("Notifications")
                    .performClick()
                onNodeWithText("Set your social battery level")
                    .assertIsDisplayed()
                saveScreen("phone-$phoneWidth-notifications-$themeName")
                onNodeWithContentDescription("Back")
                    .performClick()
                onNode(hasText("Finance") and isSelectable())
                    .assertIsSelected()
                onNodeWithText("Finance history")
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun narrowDesktopLayoutKeepsSidebar() {
        renderApplication(width = 900, height = 700, theme = SettingsTheme.LIGHT) { themeName ->
            saveScreen("desktop-narrow-dashboard-$themeName")
        }
    }

    @Test
    fun customDesktopHeaderUsesOneRowAndPreservesNavigationActions() =
        runSkikoComposeUiTest(size = Size(1440f, 900f)) {
            val settingsLocalDataSource = inMemorySettingsLocalDataSource()

            setContent {
                App(
                    settingsLocalDataSource = settingsLocalDataSource,
                    applicationTopBar = { applicationTopBarState, applicationTopBarActions ->
                        ApplicationTopBar(
                            applicationTopBarState = applicationTopBarState,
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("desktop-title-bar"),
                            actions = applicationTopBarActions,
                        )
                    },
                )
            }

            onNodeWithTag("desktop-title-bar")
                .assertHeightIsEqualTo(40.dp)
                .assertTopPositionInRootIsEqualTo(0.dp)
            onAllNodesWithText("Dashboard")
                .assertCountEquals(2)
            val dashboardGreetingBounds = onNode(hasText("Good ", substring = true))
                .getUnclippedBoundsInRoot()
            assertEquals(64.dp, dashboardGreetingBounds.top)
            onNodeWithContentDescription("Notifications")
                .performClick()
            onNodeWithText("Task “Plan the week” is overdue")
                .assertIsDisplayed()
                .performClick()
            onNodeWithTag("desktop-title-bar")
                .assert(hasAnyDescendant(hasText("Tasks")))
            openDestination("Dashboard")
            onNodeWithText("Net spent this month")
                .performClick()
            onNodeWithTag("desktop-title-bar")
                .assert(hasAnyDescendant(hasText("Finances")))
            onNode(hasText("Finances") and isSelectable())
                .assertIsSelected()
            onNodeWithContentDescription("Back")
                .assertDoesNotExist()
            openDestination("Dashboard")
            onNodeWithTag("desktop-title-bar")
                .assert(hasAnyDescendant(hasText("Dashboard")))
        }

    private fun renderApplication(
        width: Int,
        height: Int,
        theme: SettingsTheme,
        scenario: ComposeUiTest.(String) -> Unit,
    ) = runSkikoComposeUiTest(size = Size(width.toFloat(), height.toFloat())) {
        setApplicationContent(theme)
        waitForIdle()
        scenario(theme.name.lowercase())
    }

    private fun ComposeUiTest.saveScreen(name: String) {
        waitForIdle()
        val bitmap = onAllNodes(isRoot())
            .onFirst()
            .captureToImage()
        val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
        val encodedImage = checkNotNull(image.encodeToData())
        val outputDirectory = File(PREVIEW_DIRECTORY)
        assertTrue(outputDirectory.isDirectory || outputDirectory.mkdirs())
        File(outputDirectory, "$name.png").writeBytes(encodedImage.bytes)
        encodedImage.close()
        image.close()
    }
}
