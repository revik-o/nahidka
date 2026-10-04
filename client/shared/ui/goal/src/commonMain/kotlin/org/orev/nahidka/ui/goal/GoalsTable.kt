package org.orev.nahidka.ui.goal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.GoalRecord

@Composable
fun GoalsTable(
    goals: List<GoalRecord>,
    onGoalClick: (GoalRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxWidth()) {
        items(goals, key = GoalRecord::identifier) { goal ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGoalClick(goal) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(goal.title, Modifier.weight(1f))
                Text("${goal.progressPercentage.toInt()}%")
                goal.deadlineInstant?.let { deadline -> Text(deadline.toString()) }
            }
            HorizontalDivider()
        }
    }
}
