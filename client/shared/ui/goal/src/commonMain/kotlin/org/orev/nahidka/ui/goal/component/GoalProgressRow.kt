package org.orev.nahidka.ui.goal.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.GoalRecord
import org.orev.nahidka.ui.common.component.DateText

@Composable
internal fun GoalProgressRow(goal: GoalRecord) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinearProgressIndicator(
            progress = { goal.progressPercentage / GoalRecord.PROGRESS_PERCENTAGE_RANGE.endInclusive },
            modifier = Modifier.weight(1f),
        )
        goal.deadlineDate?.let { deadlineDate ->
            DateText(deadlineDate)
        }
    }
}
