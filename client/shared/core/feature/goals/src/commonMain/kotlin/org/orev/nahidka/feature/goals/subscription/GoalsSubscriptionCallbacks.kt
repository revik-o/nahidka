package org.orev.nahidka.feature.goals.subscription

import org.orev.nahidka.feature.goals.dto.GoalsDeleted
import org.orev.nahidka.feature.goals.dto.GoalsInserted
import org.orev.nahidka.feature.goals.dto.GoalsSnapshot
import org.orev.nahidka.feature.goals.dto.GoalsUpdated

internal data class GoalsSubscriptionCallbacks(
    val onSnapshot: suspend (GoalsSnapshot) -> Unit = {},
    val onUpdate: suspend (GoalsUpdated) -> Unit = {},
    val onInsert: suspend (GoalsInserted) -> Unit = {},
    val onDelete: suspend (GoalsDeleted) -> Unit = {},
    val onFailure: suspend (Throwable) -> Unit = { failure -> throw failure }
)
