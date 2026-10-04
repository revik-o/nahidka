package org.orev.nahidka.ui.tasks.board

import androidx.compose.foundation.gestures.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem

internal fun Modifier.taskCardDrag(
    taskItem: TaskItem,
    taskBoardDragState: TaskBoardDragState,
    onTaskDrop: (TaskItem, TaskStatus) -> Unit,
): Modifier = this
    .onGloballyPositioned { cardCoordinates ->
        taskBoardDragState.placeCard(taskItem.task.identifier, cardCoordinates.boundsInRoot())
    }
    .pointerInput(taskItem) {
        awaitEachGesture {
            val pointerDown = awaitFirstDown(requireUnconsumed = false)
            val dragStart = if (pointerDown.type == PointerType.Mouse) {
                awaitTouchSlopOrCancellation(pointerDown.id) { pointerChange, _ -> pointerChange.consume() }
            } else {
                awaitLongPressOrCancellation(pointerDown.id)
            }

            if (dragStart != null) {
                taskBoardDragState.start(taskItem, pointerDown.position)
                taskBoardDragState.drag(dragStart.position - pointerDown.position)

                val dragCompleted = drag(dragStart.id) { pointerChange ->
                    taskBoardDragState.drag(pointerChange.positionChange())
                    pointerChange.consume()
                }

                if (dragCompleted) {
                    taskBoardDragState.drop(onTaskDrop)
                } else {
                    taskBoardDragState.cancel()
                }
            }
        }
    }
