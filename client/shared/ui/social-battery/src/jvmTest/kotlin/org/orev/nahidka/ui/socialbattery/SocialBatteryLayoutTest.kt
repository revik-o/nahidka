package org.orev.nahidka.ui.socialbattery

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
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

private const val PREVIEW_TAG = "social-battery-preview"
private const val CHARGE_ANIMATION_MILLISECONDS = 2_000L

@OptIn(ExperimentalTestApi::class)
class SocialBatteryLayoutTest : SocialBatteryTest() {

    @Test
    fun expandedLayoutRendersEveryChargeInBothThemes() = runSkikoComposeUiTest(size = Size(1200f, 800f)) {
        val darkTheme = mutableStateOf(false)
        setContent {
            NahidkaTheme(darkTheme.value) {
                Surface {
                    SocialBatteryScreen(
                        socialBatteryViewModel,
                        Modifier
                            .size(1200.dp, 800.dp)
                            .testTag(PREVIEW_TAG),
                    )
                }
            }
        }
        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        saveScreen("desktop-unset-light")
        showCharge(0, "Drained — time to recharge")
        saveScreen("desktop-empty-light")
        showCharge(12, "Drained — time to recharge")
        saveScreen("desktop-low-light")
        showCharge(45, "Steady — something calm will do")
        saveScreen("desktop-medium-light")
        showCharge(90, "Charged — ready to socialize")
        saveScreen("desktop-high-light")
        darkTheme.value = true
        saveScreen("desktop-high-dark")
        showCharge(45, "Steady — something calm will do")
        saveScreen("desktop-medium-dark")
        showCharge(12, "Drained — time to recharge")
        saveScreen("desktop-low-dark")
        showCharge(0, "Drained — time to recharge")
        saveScreen("desktop-empty-dark")
    }

    @Test
    fun expandedUnsetBatteryRendersInDarkTheme() = runSkikoComposeUiTest(size = Size(1200f, 800f)) {
        setContent {
            NahidkaTheme(darkTheme = true) {
                Surface {
                    SocialBatteryScreen(
                        socialBatteryViewModel,
                        Modifier
                            .size(1200.dp, 800.dp)
                            .testTag(PREVIEW_TAG),
                    )
                }
            }
        }
        onNodeWithText("How charged do you feel?")
            .assertIsDisplayed()
        saveScreen("desktop-unset-dark")
    }

    @Test
    fun ukrainianCompactLayoutsFitPortraitAndLandscapePhones() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("uk"))
        try {
            renderPhone(width = 360, height = 800, name = "phone-portrait-uk")
            renderPhone(width = 800, height = 360, name = "phone-landscape-uk")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private fun renderPhone(width: Int, height: Int, name: String) =
        runSkikoComposeUiTest(size = Size(width.toFloat(), height.toFloat())) {
            socialBatteryViewModel.updateBattery(SocialBattery(100))
            setContent {
                NahidkaTheme(darkTheme = true) {
                    Surface {
                        SocialBatteryScreen(
                            socialBatteryViewModel,
                            Modifier
                                .size(width.dp, height.dp)
                                .testTag(PREVIEW_TAG),
                        )
                    }
                }
            }
            mainClock.advanceTimeBy(CHARGE_ANIMATION_MILLISECONDS)
            onNodeWithText("Заряджено — час для спілкування")
                .assertIsDisplayed()
            onNodeWithText("Торкніться батареї або потягніть її, щоб указати рівень")
                .assertIsDisplayed()
            saveScreen(name)
        }

    private fun ComposeUiTest.showCharge(percentage: Int, description: String) {
        socialBatteryViewModel.updateBattery(SocialBattery(percentage))
        mainClock.advanceTimeBy(CHARGE_ANIMATION_MILLISECONDS)
        onNodeWithText(description)
            .assertIsDisplayed()
    }

    private fun ComposeUiTest.saveScreen(name: String) {
        val bitmap = onNodeWithTag(PREVIEW_TAG).captureToImage()
        val image = Image.makeFromBitmap(bitmap.asSkiaBitmap())
        val encodedImage = checkNotNull(image.encodeToData())
        val outputDirectory = File("/tmp/nahidka-social-battery-previews")
        assertTrue(outputDirectory.isDirectory || outputDirectory.mkdirs())
        File(outputDirectory, "$name.png").writeBytes(encodedImage.bytes)
        encodedImage.close()
        image.close()
    }
}
