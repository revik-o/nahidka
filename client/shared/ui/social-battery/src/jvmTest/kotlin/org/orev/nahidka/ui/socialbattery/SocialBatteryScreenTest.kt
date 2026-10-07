package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import kotlin.test.Test
import kotlin.test.assertEquals

private const val BATTERY_TITLE = "Social battery"

@OptIn(ExperimentalTestApi::class)
class SocialBatteryScreenTest : SocialBatteryTest() {

    @Test
    fun unsetBatteryAsksForLevel() = runComposeUiTest {
        showSocialBatteryScreen()

        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        onNodeWithContentDescription(BATTERY_TITLE)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "—"))
    }

    @Test
    fun tappingBatteryTopChargesItFully() = runComposeUiTest {
        showSocialBatteryScreen()

        onNodeWithContentDescription(BATTERY_TITLE)
            .performTouchInput { click(topCenter) }

        onNodeWithText("Charged — ready to socialize")
            .assertIsDisplayed()
        assertEquals(SocialBattery(100), socialBatteryViewModel.batteryState.value.socialBattery)
    }

    @Test
    fun draggingBatteryDownDrainsIt() = runComposeUiTest {
        showSocialBatteryScreen()

        onNodeWithContentDescription(BATTERY_TITLE)
            .performTouchInput {
                down(topCenter)
                moveTo(center)
                moveTo(bottomCenter)
                up()
            }

        onNodeWithText("Drained — time to recharge")
            .assertIsDisplayed()
        assertEquals(SocialBattery(0), socialBatteryViewModel.batteryState.value.socialBattery)
    }

    @Test
    fun mouseClickAndDragUpdateBattery() = runComposeUiTest {
        showSocialBatteryScreen()

        onNodeWithContentDescription(BATTERY_TITLE)
            .performMouseInput { click(topCenter) }

        assertEquals(SocialBattery(100), socialBatteryViewModel.batteryState.value.socialBattery)

        onNodeWithContentDescription(BATTERY_TITLE)
            .performMouseInput {
                moveTo(topCenter)
                press()
                moveTo(center)
                moveTo(bottomCenter)
                release()
            }

        onNodeWithText("Drained — time to recharge")
            .assertIsDisplayed()
        assertEquals(SocialBattery(0), socialBatteryViewModel.batteryState.value.socialBattery)
    }

    @Test
    fun accessibilityActionSetsExactLevel() = runComposeUiTest {
        showSocialBatteryScreen(width = 360.dp)

        onNodeWithContentDescription(BATTERY_TITLE)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.42f) }

        onNodeWithText("Steady — something calm will do")
            .assertIsDisplayed()
        onNodeWithContentDescription(BATTERY_TITLE)
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.42f, 0f..1f, 99))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "42%"))
    }

    private fun ComposeUiTest.showSocialBatteryScreen(width: Dp = 1200.dp) {
        setContent {
            SocialBatteryScreen(
                socialBatteryViewModel,
                Modifier.size(width, 800.dp),
            )
        }
    }
}
