package org.orev.nahidka.ui.tasks.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_column_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.CardList
import org.orev.nahidka.ui.tasks.model.TaskItem

@Composable
internal fun TaskCardList(
    taskItems: List<TaskItem>,
    modifier: Modifier = Modifier,
    taskCard: @Composable (TaskItem) -> Unit,
) {
    CardList(
        listItems = taskItems,
        itemKey = { taskItem -> taskItem.task.identifier },
        emptyListMessage = stringResource(Res.string.tasks_column_empty),
        modifier = modifier,
        itemCard = taskCard,
    )
}
