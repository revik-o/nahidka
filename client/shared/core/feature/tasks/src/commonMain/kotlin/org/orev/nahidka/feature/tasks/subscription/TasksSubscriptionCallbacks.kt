package org.orev.nahidka.feature.tasks.subscription

import org.orev.nahidka.feature.tasks.dto.TasksDeleted
import org.orev.nahidka.feature.tasks.dto.TasksInserted
import org.orev.nahidka.feature.tasks.dto.TasksSnapshot
import org.orev.nahidka.feature.tasks.dto.TasksUpdated

internal data class TasksSubscriptionCallbacks(
    val onSnapshot: suspend (TasksSnapshot) -> Unit = {},
    val onUpdate: suspend (TasksUpdated) -> Unit = {},
    val onDelete: suspend (TasksDeleted) -> Unit = {},
    val onInsert: suspend (TasksInserted) -> Unit = {},
    val onFailure: suspend (Throwable) -> Unit = { failure -> throw failure }
)
