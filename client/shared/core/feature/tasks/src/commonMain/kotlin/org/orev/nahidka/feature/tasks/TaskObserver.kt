package org.orev.nahidka.feature.tasks

class TaskObserver {
    private val listeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    fun subscribe(event: String, action: (Any?) -> Unit) {
        val eventListeners = listeners.getOrPut(event) { mutableListOf() }
        eventListeners.add(action)
    }

    fun notify(event: String, data: Any? = null) {
        listeners[event]?.forEach { it(data) }
    }
}
