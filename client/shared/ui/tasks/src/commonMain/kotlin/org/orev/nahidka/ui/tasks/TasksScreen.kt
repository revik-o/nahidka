package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.tasks.board.TasksBoard
import org.orev.nahidka.ui.tasks.dialog.TaskDeletionDialog
import org.orev.nahidka.ui.tasks.dialog.TaskEditorDialog
import org.orev.nahidka.ui.tasks.dialog.TaskRatingLevelsDialog
import org.orev.nahidka.ui.tasks.list.TasksList

@Composable
fun TasksScreen(tasksViewModel: TasksViewModel, modifier: Modifier = Modifier) {
    val tasksContent by tasksViewModel.tasksContent.collectAsStateWithLifecycle()
    var selectedView by rememberSaveable { mutableStateOf(TasksView.BOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val changeRejectionMessage = stringResource(Res.string.tasks_error_unsaved)

    LaunchedEffect(tasksViewModel, changeRejectionMessage) {
        tasksViewModel.changeRejections.collect { snackbarHostState.showSnackbar(changeRejectionMessage) }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val layoutWidth = LayoutWidth.of(maxWidth)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (layoutWidth == LayoutWidth.COMPACT) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TasksHeader(
                selectedView = selectedView,
                layoutWidth = layoutWidth,
                onViewSelect = { tasksView -> selectedView = tasksView },
                onTaskCreate = tasksViewModel::openTaskCreation,
                onRatingLevelsEdit = tasksViewModel::openRatingLevelsEditing,
            )
            when (selectedView) {
                TasksView.BOARD -> TasksBoard(tasksContent, layoutWidth, tasksViewModel.taskInteractions)
                TasksView.LIST -> TasksList(tasksContent, layoutWidth, tasksViewModel.taskInteractions)
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }

    TaskEditorDialog(tasksViewModel.taskEditor, tasksContent.ratingLevels)
    TaskDeletionDialog(tasksViewModel.taskDeletion)
    TaskRatingLevelsDialog(tasksViewModel.ratingLevelsEditor, tasksViewModel::addRatingLevel)
}
