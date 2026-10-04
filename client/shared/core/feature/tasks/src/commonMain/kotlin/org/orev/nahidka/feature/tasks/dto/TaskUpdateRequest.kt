package org.orev.nahidka.feature.tasks.dto

import org.orev.nahidka.api.TaskStatus
import org.orev.nahidka.core.common.NullablePatch

data class TaskUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val descriptionPatch: NullablePatch<String> = NullablePatch.Keep,
    val status: TaskStatus? = null,
    val priority: Int? = null
)
