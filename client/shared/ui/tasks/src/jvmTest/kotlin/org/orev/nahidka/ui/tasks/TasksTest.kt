package org.orev.nahidka.ui.tasks

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.orev.nahidka.feature.tasks.dto.TaskStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class TasksTest {

    protected val tasksScreenGraph by lazy { createGraph<TasksScreenGraph>() }
    protected val tasksViewModel by lazy { tasksScreenGraph.tasksViewModel }

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun createTask(title: String, status: TaskStatus = TaskStatus.TO_DO) {
        tasksViewModel.openTaskCreation()
        tasksViewModel.taskEditor.edit { taskDraft ->
            taskDraft
                .copy(title = title)
                .withStatus(status)
        }
        tasksViewModel.taskEditor.submit()
    }
}
