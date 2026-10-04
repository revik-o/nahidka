package org.orev.nahidka.ui.tasks

import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_view_board
import nahidka.shared.ui.tasks.generated.resources.tasks_view_list
import org.jetbrains.compose.resources.StringResource

internal enum class TasksView(val title: StringResource) {
    BOARD(Res.string.tasks_view_board),
    LIST(Res.string.tasks_view_list),
}
