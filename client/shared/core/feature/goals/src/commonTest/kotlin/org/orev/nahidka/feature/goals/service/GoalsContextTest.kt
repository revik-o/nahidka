package org.orev.nahidka.feature.goals.service

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.orev.nahidka.core.common.NullablePatch
import org.orev.nahidka.feature.goals.dto.GoalCreationRequest
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.feature.goals.dto.GoalUpdateRequest
import kotlin.test.*

private const val GOAL_IDENTIFIER = "half-marathon"

class GoalsContextTest {

    private val goalsContext = GoalsContext()

    @Test
    fun createdGoalKeepsDescriptionPictureAndDeadline() = runTest {
        val deadlineDate = LocalDate(2027, 4, 18)

        goalsContext.createGoal(
            GoalCreationRequest(
                identifier = GOAL_IDENTIFIER,
                title = "Run a half marathon",
                description = "Finish 21 km",
                picture = GoalPicture.Emoji("🏃"),
                progressPercentage = 40f,
                deadlineDate = deadlineDate,
            ),
        )

        val createdGoal = goalsContext
            .currentSnapshot()
            .goals
            .single()
        assertEquals("Finish 21 km", createdGoal.description)
        assertEquals(GoalPicture.Emoji("🏃"), createdGoal.picture)
        assertEquals(deadlineDate, createdGoal.deadlineDate)
    }

    @Test
    fun updateReplacesAndClearsOptionalFields() = runTest {
        createGoal(GoalPicture.Emoji("🏃"))

        goalsContext.updateGoal(
            GoalUpdateRequest(
                identifier = GOAL_IDENTIFIER,
                description = "Finish under two hours",
                picturePatch = NullablePatch.Set(GoalPicture.Photo(byteArrayOf(1, 2, 3))),
                deadlinePatch = NullablePatch.Set(LocalDate(2027, 4, 18)),
            ),
        )
        goalsContext.updateGoal(
            GoalUpdateRequest(
                identifier = GOAL_IDENTIFIER,
                picturePatch = NullablePatch.Clear,
                deadlinePatch = NullablePatch.Clear,
            ),
        )

        val updatedGoal = goalsContext
            .currentSnapshot()
            .goals
            .single()
        assertEquals("Finish under two hours", updatedGoal.description)
        assertNull(updatedGoal.picture)
        assertNull(updatedGoal.deadlineDate)
    }

    @Test
    fun samePhotoContentIsNotAChange() = runTest {
        createGoal(GoalPicture.Photo(byteArrayOf(1, 2, 3)))

        val mutationResult = goalsContext.updateGoal(
            GoalUpdateRequest(
                identifier = GOAL_IDENTIFIER,
                picturePatch = NullablePatch.Set(GoalPicture.Photo(byteArrayOf(1, 2, 3))),
            ),
        )

        assertFalse(mutationResult.changed)
        assertEquals(1L, goalsContext.currentSnapshot().revision)
    }

    @Test
    fun blankEmojiEmptyPhotoAndOutOfRangeProgressAreRejected() = runTest {
        assertFailsWith<IllegalArgumentException> { createGoal(GoalPicture.Emoji(" ")) }
        assertFailsWith<IllegalArgumentException> { createGoal(GoalPicture.Photo(byteArrayOf())) }
        assertFailsWith<IllegalArgumentException> {
            goalsContext.createGoal(GoalCreationRequest(GOAL_IDENTIFIER, "Run", progressPercentage = 101f))
        }

        assertTrue(
            goalsContext
                .currentSnapshot()
                .goals
                .isEmpty(),
        )
    }

    private suspend fun createGoal(picture: GoalPicture) {
        goalsContext.createGoal(GoalCreationRequest(GOAL_IDENTIFIER, "Run a half marathon", picture = picture))
    }
}
