package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

private const val BATTERY_MAXIMUM_WIDTH_FRACTION = 0.6f
private const val BATTERY_MAXIMUM_HEIGHT_FRACTION = 0.74f
private const val PARTICLE_FIELD_WIDTH_SCALE = 2.4f
private const val PARTICLE_FIELD_HEIGHT_SCALE = 1.35f

@Composable
internal fun BatteryField(
    socialBattery: SocialBattery?,
    chargeColor: Color,
    onBatteryChange: (SocialBattery) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chargeFraction by animateFloatAsState(socialBattery.chargeFraction())

    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val batteryHeight = minOf(
            maxHeight * BATTERY_MAXIMUM_HEIGHT_FRACTION,
            maxWidth * BATTERY_MAXIMUM_WIDTH_FRACTION / BATTERY_ASPECT_RATIO,
        )
        val batterySize = DpSize(batteryHeight * BATTERY_ASPECT_RATIO, batteryHeight)

        BatteryParticles(
            chargeFraction = chargeFraction,
            chargeColor = chargeColor,
            batterySize = batterySize,
            modifier = Modifier.size(batterySize.width * PARTICLE_FIELD_WIDTH_SCALE, batterySize.height * PARTICLE_FIELD_HEIGHT_SCALE),
        )
        BatteryGauge(
            socialBattery = socialBattery,
            chargeFraction = chargeFraction,
            chargeColor = chargeColor,
            onBatteryChange = onBatteryChange,
            modifier = Modifier.size(batterySize),
        )
    }
}
