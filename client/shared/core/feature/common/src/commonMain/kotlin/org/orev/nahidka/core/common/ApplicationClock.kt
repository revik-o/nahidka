package org.orev.nahidka.core.common

import kotlin.time.Instant

fun interface ApplicationClock {
    fun now(): Instant
}
