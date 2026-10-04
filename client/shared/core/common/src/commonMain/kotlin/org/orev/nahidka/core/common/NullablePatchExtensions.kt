package org.orev.nahidka.core.common

fun <T> NullablePatch<T>.applyTo(current: T?): T? = when (this) {
    NullablePatch.Clear -> null
    NullablePatch.Keep -> current
    is NullablePatch.Set -> value
}

fun <T> nullablePatch(previous: T?, current: T?): NullablePatch<T> = when {
    previous == current -> NullablePatch.Keep
    current == null -> NullablePatch.Clear
    else -> NullablePatch.Set(current)
}
