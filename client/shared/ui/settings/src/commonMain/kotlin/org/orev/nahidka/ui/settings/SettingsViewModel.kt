package org.orev.nahidka.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.orev.nahidka.feature.settings.dto.Settings
import org.orev.nahidka.feature.settings.dto.SettingsSnapshot
import org.orev.nahidka.feature.settings.service.SettingsContext
import org.orev.nahidka.feature.settings.service.SettingsManager

@Inject
class SettingsViewModel(
    settingsContext: SettingsContext,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val mutableSaveFailed = MutableStateFlow(false)

    val settingsState: StateFlow<SettingsSnapshot> = settingsContext.settingsState

    val saveFailed: StateFlow<Boolean> = mutableSaveFailed.asStateFlow()

    fun saveSettings(settings: Settings) {
        viewModelScope.launch {
            mutableSaveFailed.value = try {
                settingsManager.saveSettings(settings)
                false
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                true
            }
        }
    }
}
