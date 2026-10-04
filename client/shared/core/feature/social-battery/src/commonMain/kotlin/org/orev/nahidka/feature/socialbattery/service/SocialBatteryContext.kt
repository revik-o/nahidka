package org.orev.nahidka.feature.socialbattery.service

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery
import org.orev.nahidka.feature.socialbattery.dto.SocialBatteryMutationResult
import org.orev.nahidka.feature.socialbattery.dto.SocialBatterySnapshot

class SocialBatteryContext(initialSocialBattery: SocialBattery? = null) {

    private val stateMutex = Mutex()
    private val mutableBatteryState = MutableStateFlow(SocialBatterySnapshot(0, initialSocialBattery))

    val batteryState = mutableBatteryState.asStateFlow()

    fun currentSnapshot(): SocialBatterySnapshot = batteryState.value

    internal suspend fun applyBatteryUpdate(socialBattery: SocialBattery): SocialBatteryMutationResult =
        stateMutex.withLock {
            currentCoroutineContext().ensureActive()
            val previous = batteryState.value

            if (previous.socialBattery == socialBattery) {
                return@withLock SocialBatteryMutationResult(previous.revision, socialBattery, false)
            }

            val nextRevision = incrementRevision(previous.revision)
            mutableBatteryState.value = SocialBatterySnapshot(nextRevision, socialBattery)

            SocialBatteryMutationResult(nextRevision, socialBattery, true)
        }
}
