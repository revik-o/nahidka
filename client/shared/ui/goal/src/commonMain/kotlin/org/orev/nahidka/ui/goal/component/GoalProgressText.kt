package org.orev.nahidka.ui.goal.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_progress_completed
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalRecord
import kotlin.math.roundToInt

@Composable
internal fun GoalProgressText(goal: GoalRecord) {
    Text(
        text = stringResource(Res.string.goal_progress_completed, goal.progressPercentage.roundToInt()),
        style = MaterialTheme.typography.labelLarge,
        color = if (goal.completed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
    )
}
