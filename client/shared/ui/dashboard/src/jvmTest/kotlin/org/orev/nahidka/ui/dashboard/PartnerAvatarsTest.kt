package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import java.io.File
import javax.imageio.ImageIO

class PartnerAvatarsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun presenceUpdatesWithoutReopeningDashboard() {
        val store = PartnerPresenceStore()
        val alex = PartnerPresence("alex", "Alex", "A", isOnline = true)
        val sam = PartnerPresence("sam", "Sam", "S", isOnline = true)
        store.replace(listOf(alex, sam))
        compose.setContent {
            val partners by store.partners.collectAsState()
            NahidkaTheme(darkTheme = true) {
                Surface {
                    PartnerAvatars(partners, Modifier.padding(16.dp))
                }
            }
        }
        compose.onNodeWithContentDescription("Alex is online").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sam is online").assertIsDisplayed()
        ImageIO.write(compose.onRoot().captureToImage().toAwtImage(), "png", File("/tmp/nahidka-partner-avatars.png"))

        compose.runOnIdle { store.replace(listOf(alex.copy(isOnline = false), sam)) }
        compose.onNodeWithContentDescription("Alex is offline").assertIsDisplayed()
        compose.onNodeWithContentDescription("Alex is online").assertDoesNotExist()
        compose.onNodeWithContentDescription("Sam is online").assertIsDisplayed()

        compose.runOnIdle { store.replace(emptyList()) }
        compose.onNodeWithContentDescription("Alex is offline").assertDoesNotExist()
        compose.onNodeWithContentDescription("Sam is online").assertDoesNotExist()
    }
}
