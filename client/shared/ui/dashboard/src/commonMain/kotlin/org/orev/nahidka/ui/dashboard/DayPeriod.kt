package org.orev.nahidka.ui.dashboard

import nahidka.shared.ui.dashboard.generated.resources.*
import org.jetbrains.compose.resources.StringResource

enum class DayPeriod(val startHour: Int, val greeting: StringResource) {
    MORNING(startHour = 5, greeting = Res.string.dashboard_greeting_morning),
    AFTERNOON(startHour = 12, greeting = Res.string.dashboard_greeting_afternoon),
    EVENING(startHour = 18, greeting = Res.string.dashboard_greeting_evening);

    companion object {

        fun of(hour: Int): DayPeriod =
            entries.lastOrNull { dayPeriod -> hour >= dayPeriod.startHour } ?: EVENING
    }
}
