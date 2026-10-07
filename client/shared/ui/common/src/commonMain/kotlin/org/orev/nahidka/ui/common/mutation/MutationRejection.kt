package org.orev.nahidka.ui.common.mutation

suspend fun mutationRejectionOf(mutation: suspend () -> Unit): IllegalArgumentException? =
    try {
        mutation()
        null
    } catch (rejection: IllegalArgumentException) {
        rejection
    }
