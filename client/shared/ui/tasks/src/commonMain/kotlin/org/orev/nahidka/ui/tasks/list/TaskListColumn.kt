package org.orev.nahidka.ui.tasks.list

import nahidka.shared.ui.common.generated.resources.common_field_description
import nahidka.shared.ui.common.generated.resources.common_field_title
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

internal enum class TaskListColumn(val title: StringResource, val weight: Float) {
    TITLE(CommonResources.string.common_field_title, 2f),
    DESCRIPTION(CommonResources.string.common_field_description, 3f),
    STATUS(Res.string.tasks_field_status, 1.4f),
    DUE_DATE(Res.string.tasks_field_due_date, 1.2f),
    RATING(Res.string.tasks_field_rating, 1.4f),
}
