package org.orev.nahidka.ui.financialmanagement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import dev.zacsweers.metro.createGraph
import org.jetbrains.skia.Image
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import java.io.File
import kotlin.test.Test
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
internal class FinancialManagementScreenTest {

    private val financialManagementTestGraph = createGraph<FinancialManagementTestGraph>()
    private lateinit var composeRule: ComposeUiTest

    @Test
    fun expandedHistoryPlanningAndDialogs() {
        runDesktopComposeUiTest(width = 1000, height = 760) {
            composeRule = this
            verifyScreen(1000, "expanded")
        }
    }

    @Test
    fun compactHistoryPlanningAndDialogs() {
        runDesktopComposeUiTest(width = 400, height = 760) {
            composeRule = this
            verifyScreen(400, "compact")
        }
    }

    private fun verifyScreen(width: Int, layoutName: String) {
        runBlocking {
            FinancialDemoData.populate(
                financialManagementTestGraph.financialGateway,
                ApplicationClock { Instant.parse("2026-10-04T12:00:00Z") },
            )
        }
        val historyViewModel = financialManagementTestGraph.financialHistoryViewModel
        val planningViewModel = financialManagementTestGraph.financialPlanningViewModel
        composeRule.setContent {
            MaterialTheme {
                Surface {
                    Box(Modifier.requiredSize(width.dp, 760.dp)) {
                        FinancialManagementScreen(historyViewModel, planningViewModel)
                    }
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Food").fetchSemanticsNodes().isNotEmpty()
        }
        capture("$layoutName-history")
        if (width < 600) {
            composeRule.onNodeWithContentDescription("Add financial operation").performClick()
        } else {
            composeRule.onNodeWithText("Add financial operation").performClick()
        }
        composeRule.onNodeWithText("New financial operation").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        capture("$layoutName-operation-editor")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Finance planning").performClick()
        composeRule.onNodeWithText("Amount of money: 3000.00 USD").assertIsDisplayed()
        composeRule.onNodeWithText("How much is left: 2353.75 USD").assertIsDisplayed()
        capture("$layoutName-planning")

        if (width < 600) {
            composeRule.onAllNodesWithText("⋮")[0].performClick()
            composeRule.onNodeWithText("Edit").performClick()
        } else {
            composeRule.onAllNodesWithContentDescription("Edit")[0].performClick()
        }
        composeRule.onNodeWithText("Edit planning category").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsEnabled()
        capture("$layoutName-planning-editor")
        composeRule.onNodeWithText("Cancel").performClick()

        if (width < 600) {
            composeRule.onAllNodesWithText("⋮")[0].performClick()
            composeRule.onNodeWithText("Delete").performClick()
        } else {
            composeRule.onAllNodesWithContentDescription("Delete")[0].performClick()
        }
        composeRule.onNodeWithText("Delete planning category?").assertIsDisplayed()
        capture("$layoutName-deletion")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription("Next month").performClick()
        composeRule.onNodeWithText("There are no planning categories for this month yet").assertIsDisplayed()
        capture("$layoutName-empty-month")
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        val roots = composeRule.onAllNodes(isRoot())
        val bitmap = roots[roots.fetchSemanticsNodes().lastIndex]
            .captureToImage()
            .asSkiaBitmap()
        Image.makeFromBitmap(bitmap).use { image ->
            val directory = File("build/finance-screenshots")
            check(directory.isDirectory || directory.mkdirs())
            File(directory, "$name.png").writeBytes(checkNotNull(image.encodeToData()).bytes)
        }
    }
}
