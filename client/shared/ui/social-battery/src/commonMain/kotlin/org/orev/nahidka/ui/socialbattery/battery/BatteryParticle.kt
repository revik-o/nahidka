package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.ui.unit.Dp

internal class BatteryParticle(
    val direction: Float,
    val travelOffset: Float,
    val travelCycles: Int,
    val radius: Dp,
    val chargeThreshold: Float,
) {

    fun travelProgress(animationProgress: Float): Float =
        (animationProgress * travelCycles + travelOffset) % 1f
}
