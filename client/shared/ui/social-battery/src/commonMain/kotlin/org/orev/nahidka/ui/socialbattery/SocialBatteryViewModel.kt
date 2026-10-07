package org.orev.nahidka.ui.socialbattery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryContext
import org.orev.nahidka.feature.socialbattery.service.SocialBatteryManager

@Inject
class SocialBatteryViewModel(
    socialBatteryContext: SocialBatteryContext,
    private val socialBatteryManager: SocialBatteryManager,
) : ViewModel() {

    val batteryState: StateFlow<SocialBatterySnapshot> = socialBatteryContext.batteryState

    fun updateBattery(socialBattery: SocialBattery) {
        viewModelScope.launch {
            socialBatteryManager.updateBattery(socialBattery)
        }
    }
}
