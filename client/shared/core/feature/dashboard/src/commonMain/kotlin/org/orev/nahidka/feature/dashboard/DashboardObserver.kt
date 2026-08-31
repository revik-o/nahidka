package org.orev.nahidka.feature.dashboard

class DashboardObserver {
    private val subscribers = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    fun subscribe(event: String, action: (Any?) -> Unit) {
        val actions = subscribers.getOrPut(event) { mutableListOf() }
        actions.add(action)
    }

    fun notify(event: String, data: Any? = null) {
        subscribers[event]?.forEach { it.invoke(data) }
    }
}
