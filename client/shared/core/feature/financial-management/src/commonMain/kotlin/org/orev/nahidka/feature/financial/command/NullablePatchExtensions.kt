package org.orev.nahidka.feature.financial.command

fun <T> NullablePatch<T>.applyTo(current: T?): T? = when (this) {
    NullablePatch.Keep -> current
    is NullablePatch.Set -> value
    NullablePatch.Clear -> null
}
