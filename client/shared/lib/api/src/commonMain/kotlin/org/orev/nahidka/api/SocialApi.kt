package org.orev.nahidka.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class SocialApi {

    fun getBatteryLevel(): Deferred<SocialBattery> {
        val deferred = CompletableDeferred<SocialBattery>()
        deferred.complete(SocialBattery(100))
        return deferred
    }

    fun updateBatteryLevel(level: Int): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }
}
