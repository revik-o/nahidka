package org.orev.nahidka.core.common

fun incrementRevision(currentRevision: Long): Long {
    if (currentRevision < 0 || currentRevision == Long.MAX_VALUE) {
        throw ArithmeticOverflowException()
    }

    return currentRevision + 1
}
