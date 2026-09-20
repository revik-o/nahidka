package org.orev.nahidka.feature.socialbattery

import org.orev.nahidka.api.SocialApi

class SocialBatteryService(private val socialApi: SocialApi) {
    suspend fun updateMyState(value: Int) {
        socialApi.updateBatteryLevel(value)
    }
}
