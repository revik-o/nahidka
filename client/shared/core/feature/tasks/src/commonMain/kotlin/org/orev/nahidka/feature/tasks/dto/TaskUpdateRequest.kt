package org.orev.nahidka.feature.tasks.dto

import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch

data class TaskUpdateRequest(
    val identifier: String,
    val title: String? = null,
    val descriptionPatch: NullablePatch<String> = NullablePatch.Keep,
    val status: TaskStatus? = null,
    val priority: Int? = null,
    val dueDatePatch: NullablePatch<LocalDate> = NullablePatch.Keep,
    val ratingIdentifierPatch: NullablePatch<String> = NullablePatch.Keep
)
