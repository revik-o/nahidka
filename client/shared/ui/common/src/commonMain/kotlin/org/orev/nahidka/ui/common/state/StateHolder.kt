package org.orev.nahidka.ui.common.state

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

abstract class StateHolder<State, Event> : ViewModel() {
    abstract val state: StateFlow<State>
    abstract fun handleEvent(event: Event)
}
