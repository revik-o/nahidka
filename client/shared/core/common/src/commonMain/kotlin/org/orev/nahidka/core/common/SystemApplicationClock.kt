package org.orev.nahidka.core.common

import dev.zacsweers.metro.Inject
import kotlin.time.Clock
import kotlin.time.Instant

class SystemApplicationClock @Inject constructor() : ApplicationClock {
    override fun now(): Instant = Clock.System.now()
}
