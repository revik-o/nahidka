package org.orev.nahidka.ui.socialbattery.battery

import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import kotlin.math.roundToInt

private val FULL_CHARGE_PERCENTAGE = SocialBattery.PERCENTAGE_RANGE.last.toFloat()

internal val CHARGE_FRACTION_STEPS = SocialBattery.PERCENTAGE_RANGE.last - SocialBattery.PERCENTAGE_RANGE.first - 1

internal fun SocialBattery?.chargeFraction(): Float =
    (this?.percentage ?: SocialBattery.PERCENTAGE_RANGE.first) / FULL_CHARGE_PERCENTAGE

internal fun socialBatteryOf(chargeFraction: Float): SocialBattery =
    SocialBattery(
        (chargeFraction * FULL_CHARGE_PERCENTAGE)
            .roundToInt()
            .coerceIn(SocialBattery.PERCENTAGE_RANGE),
    )
