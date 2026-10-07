package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.orev.nahidka.ui.common.component.SummaryCard
import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import org.orev.nahidka.ui.socialbattery.battery.BATTERY_ASPECT_RATIO
import org.orev.nahidka.ui.socialbattery.battery.BatteryChargeText
import org.orev.nahidka.ui.socialbattery.battery.BatteryGauge
import org.orev.nahidka.ui.socialbattery.battery.chargeFraction

private val SUMMARY_BATTERY_HEIGHT = 112.dp

@Composable
fun SocialBatterySummaryCard(
    socialBatteryViewModel: SocialBatteryViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val batterySnapshot by socialBatteryViewModel.batteryState.collectAsStateWithLifecycle()
    val socialBattery = batterySnapshot.socialBattery

    SummaryCard(
        destination = ApplicationDestination.SOCIAL_BATTERY,
        onDestinationOpen = onDestinationOpen,
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BatteryGauge(
                socialBattery = socialBattery,
                chargeFraction = socialBattery.chargeFraction(),
                modifier = Modifier
                    .height(SUMMARY_BATTERY_HEIGHT)
                    .aspectRatio(BATTERY_ASPECT_RATIO),
            )
            BatteryChargeText(socialBattery, TextAlign.Start, Modifier.weight(1f))
        }
    }
}
