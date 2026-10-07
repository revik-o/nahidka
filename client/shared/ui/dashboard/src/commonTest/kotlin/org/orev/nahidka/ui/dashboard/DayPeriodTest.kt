package org.orev.nahidka.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals

class DayPeriodTest {

    @Test
    fun hoursMapToGreetingPeriods() {
        assertEquals(
            listOf(DayPeriod.EVENING, DayPeriod.MORNING, DayPeriod.MORNING, DayPeriod.AFTERNOON, DayPeriod.EVENING, DayPeriod.EVENING),
            listOf(4, 5, 11, 12, 18, 23).map(DayPeriod::of),
        )
    }
}
