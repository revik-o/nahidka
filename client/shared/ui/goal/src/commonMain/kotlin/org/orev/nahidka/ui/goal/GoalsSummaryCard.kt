package org.orev.nahidka.ui.goal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_list_empty
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.SupportingText
import org.orev.nahidka.ui.common.component.SummaryCard
import org.orev.nahidka.ui.common.navigation.ApplicationDestination
import org.orev.nahidka.ui.goal.component.GoalPictureView
import org.orev.nahidka.ui.goal.component.GoalProgressRow

private const val SUMMARY_GOAL_LIMIT = 3
private val SUMMARY_GOAL_PICTURE_SIZE = 40.dp

private val GOAL_FOCUS_ORDER: Comparator<GoalRecord> =
    compareBy<GoalRecord>(GoalRecord::completed)
        .thenBy(nullsLast()) { goal -> goal.deadlineDate }

@Composable
fun GoalsSummaryCard(
    goalsViewModel: GoalsViewModel,
    onDestinationOpen: (ApplicationDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val goalsSnapshot by goalsViewModel.goalsState.collectAsStateWithLifecycle()
    val goalsInFocus = goalsSnapshot.goals
        .sortedWith(GOAL_FOCUS_ORDER)
        .take(SUMMARY_GOAL_LIMIT)

    SummaryCard(
        destination = ApplicationDestination.GOALS,
        onDestinationOpen = onDestinationOpen,
        modifier = modifier,
    ) {
        if (goalsInFocus.isEmpty()) {
            SupportingText(stringResource(Res.string.goal_list_empty))
        }
        goalsInFocus.forEach { goal ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoalPictureView(goal.picture, goal.title, SUMMARY_GOAL_PICTURE_SIZE)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    GoalProgressRow(goal)
                }
            }
        }
    }
}
