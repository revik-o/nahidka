package org.orev.nahidka.ui.socialbattery.battery

import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import kotlin.test.Test
import kotlin.test.assertEquals

class BatteryChargeTest {

    @Test
    fun chargeFractionConvertsBothWays() {
        assertEquals(0f, null.chargeFraction())
        assertEquals(0.45f, SocialBattery(45).chargeFraction())
        assertEquals(SocialBattery(90), socialBatteryOf(0.904f))
        assertEquals(SocialBattery(0), socialBatteryOf(-0.2f))
        assertEquals(SocialBattery(100), socialBatteryOf(1.3f))
    }

    @Test
    fun chargeLevelsStartAtTheirMinimumPercentage() {
        assertEquals(
            listOf(BatteryCharge.LOW, BatteryCharge.LOW, BatteryCharge.MEDIUM, BatteryCharge.MEDIUM, BatteryCharge.HIGH, BatteryCharge.HIGH),
            listOf(0, 29, 30, 69, 70, 100).map { percentage -> BatteryCharge.of(SocialBattery(percentage)) },
        )
    }
}
