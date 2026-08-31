package org.orev.nahidka.ui.goal

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
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "${(goal.progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
