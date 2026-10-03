package org.orev.nahidka.feature.financial.command

sealed interface NullablePatch<out T> {
    data object Keep : NullablePatch<Nothing>
    data class Set<T>(val value: T) : NullablePatch<T>
    data object Clear : NullablePatch<Nothing>
}
