package org.orev.nahidka.ui.goal

import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.models.GoalEntity

@Composable
fun GoalLinearItem(
    goal: GoalEntity,
    onClick: (GoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(goal) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = goal.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = goal.deadlineInstant?.let { "Deadline: ${it.toString().substringBefore('T')} (UTC)" } ?: "No deadline",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { goal.progressPercentage / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "${(goal.progressPercentage).toInt()}%",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview
@Composable
fun GoalLinearItemPreview() {
    NahidkaTheme {
        GoalLinearItem(
            goal = GoalEntity(identifier = "1", title = "Learn Compose", progressPercentage = 75f, deadlineInstant = kotlin.time.Instant.parse("2026-12-31T00:00:00Z")),
            onClick = {}
        )
    }
}
