package org.orev.nahidka.feature.socialbattery

import kotlinx.coroutines.Deferred
import org.orev.nahidka.api.SocialApi

class SocialBatteryService(private val socialApi: SocialApi) {
    fun updateMyState(value: Int): Deferred<Unit> {
        return socialApi.updateBatteryLevel(value)
    }
}
