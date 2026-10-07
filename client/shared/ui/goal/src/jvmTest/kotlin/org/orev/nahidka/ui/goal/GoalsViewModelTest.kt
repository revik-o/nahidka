package org.orev.nahidka.ui.goal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.feature.goals.dto.GoalPicture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class GoalsViewModelTest : GoalsTest() {

    @Test
    fun goalCreationSavesEveryField() = runTest {
        val deadlineDate = LocalDate(2027, 4, 18)

        goalsViewModel.openGoalCreation()
        goalsViewModel.goalEditor.edit { goalDraft ->
            goalDraft.copy(
                title = "  Run a half marathon  ",
                description = "Finish 21 km",
                picture = GoalPicture.Emoji("🏃"),
                progressPercentage = 40f,
                deadlineDate = deadlineDate,
            )
        }
        goalsViewModel.goalEditor.submit()

        val createdGoal = awaitSingleGoal { true }
        assertEquals("Run a half marathon", createdGoal.title)
        assertEquals("Finish 21 km", createdGoal.description)
        assertEquals(GoalPicture.Emoji("🏃"), createdGoal.picture)
        assertEquals(40f, createdGoal.progressPercentage)
        assertEquals(deadlineDate, createdGoal.deadlineDate)
        assertNull(goalsViewModel.goalEditor.dialogState.value)
    }

    @Test
    fun goalDraftWithBlankTitleIsNotSubmittable() {
        goalsViewModel.openGoalCreation()
        goalsViewModel.goalEditor.edit { goalDraft -> goalDraft.copy(title = "  ") }

        assertFalse(checkNotNull(goalsViewModel.goalEditor.dialogState.value).submittable)
    }

    @Test
    fun goalEditingReplacesPictureWithPhotoAndUpdatesProgress() = runTest {
        createGoal("Run a half marathon", GoalPicture.Emoji("🏃"))
        val createdGoal = awaitSingleGoal { true }

        goalsViewModel.openGoalEditing(createdGoal)
        goalsViewModel.goalEditor.edit { goalDraft ->
            goalDraft.copy(picture = GoalPicture.Photo(byteArrayOf(1, 2, 3)), progressPercentage = 100f)
        }
        goalsViewModel.goalEditor.submit()

        val editedGoal = awaitSingleGoal { goal -> goal.progressPercentage == 100f }
        assertEquals(GoalPicture.Photo(byteArrayOf(1, 2, 3)), editedGoal.picture)
        assertEquals(createdGoal.identifier, editedGoal.identifier)
    }

    @Test
    fun goalDeletionRemovesGoal() = runTest {
        createGoal("Run a half marathon")
        val createdGoal = awaitSingleGoal { true }

        goalsViewModel.goalDeletion.open(createdGoal)
        goalsViewModel.goalDeletion.submit()

        goalsViewModel.goalsState.first { goalsSnapshot -> goalsSnapshot.goals.isEmpty() }
    }
}
