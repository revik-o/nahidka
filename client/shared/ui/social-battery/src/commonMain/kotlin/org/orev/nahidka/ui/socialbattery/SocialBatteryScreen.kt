package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.social_battery.generated.resources.Res
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_hint
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_unset
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.socialbattery.battery.BatteryCharge
import org.orev.nahidka.ui.socialbattery.battery.BatteryField

@Composable
fun SocialBatteryScreen(socialBatteryViewModel: SocialBatteryViewModel, modifier: Modifier = Modifier) {
    val batterySnapshot by socialBatteryViewModel.batteryState.collectAsStateWithLifecycle()
    val socialBattery = batterySnapshot.socialBattery
    val batteryCharge = socialBattery?.let(BatteryCharge::of)
    val chargeColor = batteryCharge?.color ?: MaterialTheme.colorScheme.outline

    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LayoutWidth.of(maxWidth).screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            BatteryField(
                socialBattery = socialBattery,
                chargeColor = chargeColor,
                onBatteryChange = socialBatteryViewModel::updateBattery,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            )
            Text(
                text = stringResource(batteryCharge?.description ?: Res.string.socialbattery_unset),
                color = chargeColor,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.socialbattery_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
