package org.orev.nahidka.feature.settings.service

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.orev.nahidka.core.common.incrementRevision
import org.orev.nahidka.feature.settings.dto.*

class SettingsContext(private val localDataSource: SettingsLocalDataSource) {

    private val stateMutex = Mutex()
    private val mutableSettingsState = MutableStateFlow(SettingsSnapshot(0, localDataSource.loadSettings()))
    val settingsState = mutableSettingsState.asStateFlow()

    fun currentSnapshot(): SettingsSnapshot = settingsState.value

    internal suspend fun applySettingsSave(settings: Settings): SettingsSaveResult = stateMutex.withLock {
        currentCoroutineContext().ensureActive()
        val previous = settingsState.value

        if (previous.settings == settings) {
            return@withLock SettingsSaveResult(previous.revision, settings, false)
        }

        val nextRevision = incrementRevision(previous.revision)
        localDataSource.saveSettings(settings)
        mutableSettingsState.value = SettingsSnapshot(nextRevision, settings)

        SettingsSaveResult(nextRevision, settings, true)
    }
}
