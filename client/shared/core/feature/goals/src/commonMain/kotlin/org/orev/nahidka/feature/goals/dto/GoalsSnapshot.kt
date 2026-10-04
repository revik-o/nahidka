package org.orev.nahidka.feature.goals.dto

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap

data class GoalsSnapshot(
    val revision: Long,
    private val recordsByIdentifier: PersistentMap<String, GoalRecord>
) {

    val goals: ImmutableList<GoalRecord> by lazy { recordsByIdentifier.values.toPersistentList() }

    constructor(revision: Long, goals: List<GoalRecord>) : this(
        revision, goals.associateBy { it.identifier }.toPersistentMap()
    ) {
        require(goals.size == recordsByIdentifier.size) { "Snapshot identifiers must be unique" }
    }
}
