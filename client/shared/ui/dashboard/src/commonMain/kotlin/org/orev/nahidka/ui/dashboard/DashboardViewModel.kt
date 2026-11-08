package org.orev.nahidka.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.orev.nahidka.core.common.ApplicationClock
import kotlin.time.Duration.Companion.minutes

private const val HEADER_SUBSCRIPTION_TIMEOUT_MILLISECONDS = 5_000L
private val HEADER_REFRESH_INTERVAL = 1.minutes

@Inject
class DashboardViewModel(
    private val applicationClock: ApplicationClock,
    private val timeZone: TimeZone,
    partnerPresenceStore: PartnerPresenceStore,
) : ViewModel() {

    val partners: StateFlow<List<PartnerPresence>> = partnerPresenceStore.partners

    val header: StateFlow<DashboardHeader> = flow {
        while (true) {
            emit(currentHeader())
            delay(HEADER_REFRESH_INTERVAL)
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(HEADER_SUBSCRIPTION_TIMEOUT_MILLISECONDS),
            initialValue = currentHeader(),
        )

    private fun currentHeader(): DashboardHeader {
        val localDateTime = applicationClock
            .now()
            .toLocalDateTime(timeZone)

        return DashboardHeader(DayPeriod.of(localDateTime.hour), localDateTime.date)
    }
}
