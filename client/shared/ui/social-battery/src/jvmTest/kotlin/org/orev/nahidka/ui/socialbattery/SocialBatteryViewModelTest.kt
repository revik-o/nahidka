package org.orev.nahidka.ui.socialbattery

import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import kotlin.test.Test
import kotlin.test.assertEquals

class SocialBatteryViewModelTest : SocialBatteryTest() {

    @Test
    fun batteryStartsUnset() {
        assertEquals(SocialBatterySnapshot(0, null), socialBatteryViewModel.batteryState.value)
    }

    @Test
    fun updatingBatteryPublishesOneRevisionPerChange() {
        socialBatteryViewModel.updateBattery(SocialBattery(80))
        socialBatteryViewModel.updateBattery(SocialBattery(80))
        socialBatteryViewModel.updateBattery(SocialBattery(0))

        assertEquals(SocialBatterySnapshot(2, SocialBattery(0)), socialBatteryViewModel.batteryState.value)
    }
}
