package org.orev.nahidka.ui.goal

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.GoalPicture
import kotlin.test.Test
import kotlin.test.assertEquals

private const val GOAL_TITLE = "Run a half marathon"

@OptIn(ExperimentalTestApi::class)
class GoalsScreenTest : GoalsTest() {

    @Test
    fun creationDialogSavesTitleDescriptionEmojiAndProgress() = runComposeUiTest {
        showGoalsScreen(width = 1200.dp)
        onNodeWithText("Create goal")
            .performClick()
        onNodeWithText("Save")
            .assertIsNotEnabled()
        onNodeWithText("Title")
            .performTextInput(GOAL_TITLE)
        onNodeWithText("Description")
            .performTextInput("Finish 21 km")
        onNodeWithText("🏃")
            .performClick()
        onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(50f) }
        onNodeWithText("Progress: 50%")
            .assertIsDisplayed()
        onNodeWithText("Save")
            .performClick()

        onNodeWithText("Completed on 50%")
            .assertIsDisplayed()
        waitUntil { currentGoal()?.picture == GoalPicture.Emoji("🏃") }
        assertEquals("Finish 21 km", currentGoal()?.description)
    }

    @Test
    fun clickingCardOpensGoalEditor() = runComposeUiTest {
        createGoal(GOAL_TITLE)
        showGoalsScreen(width = 1200.dp)

        onNodeWithText(GOAL_TITLE)
            .performClick()

        onNodeWithText("Edit goal")
            .assertIsDisplayed()
    }

    @Test
    fun choosingSelectedEmojiAgainRemovesPicture() = runComposeUiTest {
        createGoal(GOAL_TITLE, GoalPicture.Emoji("🎯"))
        showGoalsScreen(width = 1200.dp)
        onNodeWithText(GOAL_TITLE)
            .performClick()

        onAllNodesWithText("🎯")
            .onLast()
            .performClick()
        onNodeWithText("Save")
            .performClick()

        waitUntil { currentGoal()?.picture == null }
    }

    @Test
    fun deletingGoalRequiresConfirmationAndCanBeCancelled() = runComposeUiTest {
        createGoal(GOAL_TITLE)
        showGoalsScreen(width = 400.dp)

        openGoalMenuAndChooseDelete()
        onNodeWithText("“$GOAL_TITLE” will be deleted.")
            .assertIsDisplayed()
        onNodeWithText("Cancel")
            .performClick()
        onNodeWithText(GOAL_TITLE)
            .assertIsDisplayed()

        openGoalMenuAndChooseDelete()
        onNodeWithText("Delete")
            .performClick()

        onNodeWithText("There are no goals yet")
            .assertIsDisplayed()
    }

    @Test
    fun compactCreateButtonShowsIconAndOpensCreationDialog() = runComposeUiTest {
        createGoal(GOAL_TITLE)
        showGoalsScreen(width = 360.dp)

        onNodeWithContentDescription("Create goal")
            .performClick()

        onNodeWithText("New goal")
            .assertIsDisplayed()
    }

    private fun ComposeUiTest.openGoalMenuAndChooseDelete() {
        onNodeWithContentDescription("More actions")
            .performClick()
        onNodeWithText("Delete")
            .performClick()
    }

    private fun ComposeUiTest.showGoalsScreen(width: Dp) {
        setContent {
            GoalsScreen(goalsViewModel, Modifier.size(width, 800.dp))
        }
    }
}
