package org.orev.nahidka.core.common

import dev.zacsweers.metro.Inject

class NoOpErrorReporter @Inject constructor() : ErrorReporter {
    override fun report(failure: Throwable) = Unit
}
