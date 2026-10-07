package org.orev.nahidka.ui.goal

import dev.zacsweers.metro.createGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.dto.GoalRecord
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class GoalsTest {

    protected val goalsScreenGraph by lazy { createGraph<GoalsScreenGraph>() }
    protected val goalsViewModel by lazy { goalsScreenGraph.goalsViewModel }

    @BeforeTest
    fun replaceMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    protected fun createGoal(title: String, picture: GoalPicture? = null) {
        goalsViewModel.openGoalCreation()
        goalsViewModel.goalEditor.edit { goalDraft -> goalDraft.copy(title = title, picture = picture) }
        goalsViewModel.goalEditor.submit()
    }

    protected fun currentGoal(): GoalRecord? =
        goalsViewModel.goalsState.value.goals
            .singleOrNull()

    protected suspend fun awaitSingleGoal(goalExpectation: (GoalRecord) -> Boolean): GoalRecord =
        goalsViewModel.goalsState
            .first { goalsSnapshot ->
                goalsSnapshot.goals
                    .singleOrNull()
                    ?.let(goalExpectation) == true
            }
            .goals
            .single()
}
