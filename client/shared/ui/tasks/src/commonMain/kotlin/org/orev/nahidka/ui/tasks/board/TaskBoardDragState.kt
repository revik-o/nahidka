package org.orev.nahidka.ui.tasks.board

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import org.orev.nahidka.ui.tasks.model.TaskItem

@Stable
internal class TaskBoardDragState {

    private val columnBoundsByStatus = mutableMapOf<TaskStatus, Rect>()
    private val cardBoundsByTaskIdentifier = mutableMapOf<String, Rect>()
    private var boardPosition = Offset.Zero
    private var grabPosition = Offset.Zero
    private var dragDistance by mutableStateOf(Offset.Zero)

    var draggedTaskItem by mutableStateOf<TaskItem?>(null)
        private set

    var draggedCardBounds by mutableStateOf(Rect.Zero)
        private set

    val hoveredStatus: TaskStatus? by derivedStateOf {
        val pointerPosition = draggedCardBounds.topLeft + grabPosition + dragDistance

        columnBoundsByStatus.entries
            .firstOrNull { (_, columnBounds) -> columnBounds.contains(pointerPosition) }
            ?.key
            .takeIf { draggedTaskItem != null }
    }

    val draggedCardOffset: IntOffset
        get() = (draggedCardBounds.topLeft + dragDistance - boardPosition).round()

    fun isDragged(taskItem: TaskItem): Boolean =
        draggedTaskItem?.task?.identifier == taskItem.task.identifier

    fun placeBoard(boardPosition: Offset) {
        this.boardPosition = boardPosition
    }

    fun placeColumn(status: TaskStatus, columnBounds: Rect) {
        columnBoundsByStatus[status] = columnBounds
    }

    fun placeCard(taskIdentifier: String, cardBounds: Rect) {
        cardBoundsByTaskIdentifier[taskIdentifier] = cardBounds
    }

    fun start(taskItem: TaskItem, grabPosition: Offset) {
        this.grabPosition = grabPosition
        draggedCardBounds = cardBoundsByTaskIdentifier[taskItem.task.identifier] ?: Rect.Zero
        dragDistance = Offset.Zero
        draggedTaskItem = taskItem
    }

    fun drag(dragAmount: Offset) {
        dragDistance += dragAmount
    }

    fun drop(onTaskDrop: (TaskItem, TaskStatus) -> Unit) {
        val droppedTaskItem = draggedTaskItem
        val targetStatus = hoveredStatus

        if (droppedTaskItem != null && targetStatus != null && targetStatus != droppedTaskItem.task.status) {
            onTaskDrop(droppedTaskItem, targetStatus)
        }

        cancel()
    }

    fun cancel() {
        draggedTaskItem = null
        dragDistance = Offset.Zero
    }
}
