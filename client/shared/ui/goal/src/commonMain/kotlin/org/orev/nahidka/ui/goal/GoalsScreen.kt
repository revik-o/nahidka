package org.orev.nahidka.ui.goal

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_create
import nahidka.shared.ui.goal.generated.resources.goal_list_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.AddButton
import org.orev.nahidka.ui.common.component.CardList
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.goal.card.GoalCard
import org.orev.nahidka.ui.goal.dialog.GoalDeletionDialog
import org.orev.nahidka.ui.goal.dialog.GoalEditorDialog

@Composable
fun GoalsScreen(goalsViewModel: GoalsViewModel, modifier: Modifier = Modifier) {
    val goalsSnapshot by goalsViewModel.goalsState.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val layoutWidth = LayoutWidth.of(maxWidth)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(layoutWidth.screenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AddButton(stringResource(Res.string.goal_create), layoutWidth, goalsViewModel::openGoalCreation)
            }
            CardList(
                listItems = goalsSnapshot.goals,
                itemKey = GoalRecord::identifier,
                emptyListMessage = stringResource(Res.string.goal_list_empty),
            ) { goal ->
                GoalCard(goal, layoutWidth, goalsViewModel::openGoalEditing, goalsViewModel.goalDeletion::open)
            }
        }
    }

    GoalEditorDialog(goalsViewModel.goalEditor)
    GoalDeletionDialog(goalsViewModel.goalDeletion)
}
