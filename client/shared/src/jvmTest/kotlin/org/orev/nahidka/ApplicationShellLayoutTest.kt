package org.orev.nahidka

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import org.jetbrains.skia.Image
import org.orev.nahidka.feature.settings.dto.SettingsTheme
import java.io.File
import kotlin.test.Test
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
                    openDestination("Dashboard")
                    onNodeWithText("Net spent this month")
                        .performClick()
                    onNodeWithText("Finance history")
                        .assertIsDisplayed()
                    saveScreen("desktop-finance-$themeName")
                }
            }
    }

    @Test
    fun phoneLayoutRendersFloatingNavigationAndNotificationsScreen() {
        renderApplication(width = 390, height = 844, theme = SettingsTheme.DARK) { themeName ->
            saveScreen("phone-dashboard-$themeName")
            onNodeWithContentDescription("Notifications")
                .performClick()
            onNodeWithText("Set your social battery level")
                .assertIsDisplayed()
            saveScreen("phone-notifications-$themeName")
            onNodeWithContentDescription("Back")
                .performClick()
            openDestination("Battery")
            saveScreen("phone-social-battery-$themeName")
            openDestination("Goals")
            saveScreen("phone-goals-$themeName")
        }
    }

    @Test
    fun narrowDesktopLayoutKeepsSidebar() {
        renderApplication(width = 900, height = 700, theme = SettingsTheme.LIGHT) { themeName ->
            saveScreen("desktop-narrow-dashboard-$themeName")
        }
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
