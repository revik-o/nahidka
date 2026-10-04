package org.orev.nahidka.core.common

fun interface ErrorReporter {
    fun report(failure: Throwable)
}
