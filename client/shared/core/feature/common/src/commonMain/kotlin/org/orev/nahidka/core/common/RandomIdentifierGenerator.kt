package org.orev.nahidka.core.common

import dev.zacsweers.metro.Inject
import kotlin.uuid.Uuid

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class RandomIdentifierGenerator @Inject constructor() : IdentifierGenerator {
    override fun next(): String = Uuid.random().toString()
}
