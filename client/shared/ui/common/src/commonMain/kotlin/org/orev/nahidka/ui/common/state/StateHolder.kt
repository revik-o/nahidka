package org.orev.nahidka.ui.common.state

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

abstract class StateHolder<S, E> : ViewModel() {
    abstract val state: StateFlow<S>
    abstract fun handleEvent(event: E)
}
