package org.orev.nahidka.api

class SocialApi {

    suspend fun getBatteryLevel(): SocialBattery {
        return SocialBattery(100)
    }

    suspend fun updateBatteryLevel(level: Int) {
    }
}
