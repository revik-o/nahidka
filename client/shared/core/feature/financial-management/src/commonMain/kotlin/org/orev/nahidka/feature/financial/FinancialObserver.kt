package org.orev.nahidka.feature.financial

class FinancialObserver {
    private val listeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    fun subscribe(event: String, action: (Any?) -> Unit) {
        val actions = listeners.getOrPut(event) { mutableListOf() }
        actions.add(action)
    }

    fun notify(event: String, data: Any? = null) {
        listeners[event]?.forEach { it.invoke(data) }
    }
}
