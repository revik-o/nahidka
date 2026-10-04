package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import nahidka.shared.ui.tasks.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.tasks.dto.TaskStatus

internal val TaskStatus.title: StringResource
    get() = when (this) {
        TaskStatus.TO_DO -> Res.string.tasks_status_to_do
        TaskStatus.IN_PROGRESS -> Res.string.tasks_status_in_progress
        TaskStatus.DONE -> Res.string.tasks_status_done
    }

@Composable
internal fun taskStatusTitleWithCount(status: TaskStatus, taskCount: Int): String =
    stringResource(Res.string.tasks_status_count, stringResource(status.title), taskCount)
