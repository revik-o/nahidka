package org.orev.nahidka.feature.socialbattery.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SocialBatteryTest {

    @Test
    fun percentageMustStayWithinRange() {
        assertEquals(0..100, SocialBattery.PERCENTAGE_RANGE)
        assertEquals(100, SocialBattery(100).percentage)
        assertFailsWith<IllegalArgumentException> { SocialBattery(-1) }
        assertFailsWith<IllegalArgumentException> { SocialBattery(101) }
    }
}
