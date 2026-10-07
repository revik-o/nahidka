package org.orev.nahidka.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.core.common.ApplicationClock
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.feature.goals.service.GoalsRepository
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.tasks.dto.TaskRecord
import org.orev.nahidka.feature.tasks.service.TasksRepository
import org.orev.nahidka.ui.notification.reminder.Reminder
import org.orev.nahidka.ui.notification.reminder.remindersOf

private const val REMINDERS_SUBSCRIPTION_TIMEOUT_MILLISECONDS = 5_000L

@Inject
class NotificationsViewModel(
    tasksRepository: TasksRepository,
    goalsRepository: GoalsRepository,
    socialBatteryContext: SocialBatteryContext,
    private val applicationClock: ApplicationClock,
    private val timeZone: TimeZone,
) : ViewModel() {

    private val dismissedReminderIdentifiers = MutableStateFlow<PersistentSet<String>>(persistentSetOf())

    val reminders: StateFlow<ImmutableList<Reminder>> = combine(
        tasksRepository.tasksState,
        goalsRepository.goalsState,
        socialBatteryContext.batteryState,
        dismissedReminderIdentifiers,
    ) { tasksSnapshot, goalsSnapshot, socialBatterySnapshot, dismissedIdentifiers ->
        activeReminders(tasksSnapshot.tasks, goalsSnapshot.goals, socialBatterySnapshot, dismissedIdentifiers)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(REMINDERS_SUBSCRIPTION_TIMEOUT_MILLISECONDS),
            initialValue = activeReminders(
                tasksRepository.tasksState.value.tasks,
                goalsRepository.goalsState.value.goals,
                socialBatteryContext.batteryState.value,
                dismissedReminderIdentifiers.value,
            ),
        )

    fun dismiss(reminder: Reminder) {
        dismissedReminderIdentifiers.update { dismissedIdentifiers -> dismissedIdentifiers.adding(reminder.identifier) }
    }

    private fun activeReminders(
        tasks: List<TaskRecord>,
        goals: List<GoalRecord>,
        socialBatterySnapshot: SocialBatterySnapshot,
        dismissedIdentifiers: PersistentSet<String>,
    ): ImmutableList<Reminder> =
        remindersOf(tasks, goals, socialBatterySnapshot, today())
            .filterNot { reminder -> reminder.identifier in dismissedIdentifiers }
            .toPersistentList()

    private fun today(): LocalDate =
        applicationClock
            .now()
            .toLocalDateTime(timeZone)
            .date
}
