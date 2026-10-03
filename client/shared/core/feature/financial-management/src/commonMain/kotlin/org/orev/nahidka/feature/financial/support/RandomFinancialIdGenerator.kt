package org.orev.nahidka.feature.financial.support

import dev.zacsweers.metro.Inject
import kotlin.random.Random

class RandomFinancialIdGenerator @Inject constructor() : FinancialIdGenerator {
    override fun next(): String = "local:${Random.nextLong().toULong().toString(16)}${Random.nextLong().toULong().toString(16)}"
}
