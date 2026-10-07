package org.orev.nahidka.ui.goal.card

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.component.editingMenuActions
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.goal.component.GoalPictureView
import org.orev.nahidka.ui.goal.component.GoalProgressRow
import org.orev.nahidka.ui.goal.component.GoalProgressText

private const val GOAL_CARD_DESCRIPTION_MAXIMUM_LINES = 3

@Composable
internal fun GoalCard(
    goal: GoalRecord,
    layoutWidth: LayoutWidth,
    onGoalEdit: (GoalRecord) -> Unit,
    onGoalDelete: (GoalRecord) -> Unit,
) {
    OutlinedCard(
        onClick = { onGoalEdit(goal) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GoalPictureView(goal.picture, goal.title, layoutWidth.goalPictureSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (layoutWidth == LayoutWidth.EXPANDED) {
                        GoalProgressText(goal)
                    }
                    MoreActionsMenu(
                        editingMenuActions(
                            onEdit = { onGoalEdit(goal) },
                            onDelete = { onGoalDelete(goal) },
                        ),
                    )
                }
                Column(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (layoutWidth == LayoutWidth.COMPACT) {
                        GoalProgressText(goal)
                    }
                    if (goal.description.isNotBlank()) {
                        Text(
                            text = goal.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = GOAL_CARD_DESCRIPTION_MAXIMUM_LINES,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    GoalProgressRow(goal)
                }
            }
        }
    }
}

private val LayoutWidth.goalPictureSize: Dp
    get() = when (this) {
        LayoutWidth.COMPACT -> 64.dp
        LayoutWidth.EXPANDED -> 112.dp
    }
