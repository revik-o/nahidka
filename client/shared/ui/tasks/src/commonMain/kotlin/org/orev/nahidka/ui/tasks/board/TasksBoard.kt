package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.layout.*
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.component.taskStatusTitleWithCount
import org.orev.nahidka.ui.tasks.model.TaskInteractions
import org.orev.nahidka.ui.tasks.model.TasksContent

private val TASK_BOARD_MAXIMUM_WIDTH = 1200.dp
private const val DRAGGED_TASK_CARD_SOURCE_ALPHA = 0.4f
private const val DRAGGED_TASK_CARD_ROTATION_DEGREES = 2f

@Composable
internal fun TasksBoard(
    tasksContent: TasksContent,
    layoutWidth: LayoutWidth,
    taskInteractions: TaskInteractions,
) {
    when (layoutWidth) {
        LayoutWidth.COMPACT -> CompactTasksBoard(tasksContent, taskInteractions)
        LayoutWidth.EXPANDED -> ExpandedTasksBoard(tasksContent, taskInteractions)
    }
}

@Composable
private fun CompactTasksBoard(tasksContent: TasksContent, taskInteractions: TaskInteractions) {
    var selectedStatus by rememberSaveable { mutableStateOf(TaskStatus.TO_DO) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryScrollableTabRow(selectedTabIndex = selectedStatus.ordinal, edgePadding = 0.dp) {
            TaskStatus.entries.forEach { status ->
                Tab(
                    selected = status == selectedStatus,
                    onClick = { selectedStatus = status },
                    text = { Text(taskStatusTitleWithCount(status, tasksContent.taskItemsWithStatus(status).size)) },
                )
            }
        }
        TaskCardList(tasksContent.taskItemsWithStatus(selectedStatus)) { taskItem ->
            TaskCard(taskItem, tasksContent.ratingLevels, taskInteractions)
        }
    }
}

@Composable
private fun ExpandedTasksBoard(tasksContent: TasksContent, taskInteractions: TaskInteractions) {
    val taskBoardDragState = remember { TaskBoardDragState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { boardCoordinates ->
                taskBoardDragState.placeBoard(boardCoordinates.positionInRoot())
            },
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = TASK_BOARD_MAXIMUM_WIDTH)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TaskStatus.entries.forEach { status ->
                val statusTaskItems = tasksContent.taskItemsWithStatus(status)

                TaskBoardColumn(
                    status = status,
                    taskCount = statusTaskItems.size,
                    dropTarget = taskBoardDragState.hoveredStatus == status,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { columnCoordinates ->
                            taskBoardDragState.placeColumn(status, columnCoordinates.boundsInRoot())
                        },
                ) {
                    TaskCardList(statusTaskItems) { taskItem ->
                        TaskCard(
                            taskItem = taskItem,
                            ratingLevels = tasksContent.ratingLevels,
                            taskInteractions = taskInteractions,
                            modifier = Modifier
                                .taskCardDrag(taskItem, taskBoardDragState, taskInteractions.onTaskMove)
                                .alpha(
                                    if (taskBoardDragState.isDragged(taskItem)) DRAGGED_TASK_CARD_SOURCE_ALPHA else 1f,
                                ),
                        )
                    }
                }
            }
        }
        taskBoardDragState.draggedTaskItem?.let { draggedTaskItem ->
            TaskCard(
                taskItem = draggedTaskItem,
                ratingLevels = tasksContent.ratingLevels,
                taskInteractions = taskInteractions,
                modifier = Modifier
                    .zIndex(1f)
                    .offset { taskBoardDragState.draggedCardOffset }
                    .width(with(LocalDensity.current) { taskBoardDragState.draggedCardBounds.width.toDp() })
                    .rotate(DRAGGED_TASK_CARD_ROTATION_DEGREES),
            )
        }
    }
}
