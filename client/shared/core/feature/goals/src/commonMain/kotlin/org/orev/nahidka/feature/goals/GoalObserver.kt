package org.orev.nahidka.feature.goals

class GoalObserver {
    private val listeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    fun subscribe(event: String, action: (Any?) -> Unit) {
        val list = listeners.getOrPut(event) { mutableListOf() }
        list.add(action)
    }

    fun notify(event: String, data: Any?) {
        listeners[event]?.forEach { it(data) }
    }
}
