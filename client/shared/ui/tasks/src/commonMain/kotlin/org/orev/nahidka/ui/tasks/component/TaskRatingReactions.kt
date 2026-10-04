package org.orev.nahidka.ui.tasks.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel

@Composable
internal fun TaskRatingReactions(
    ratingLevels: List<TaskRatingLevel>,
    selectedRatingLevel: TaskRatingLevel?,
    onRatingLevelSelect: (TaskRatingLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ratingLevels.forEach { ratingLevel ->
            val selected = ratingLevel.identifier == selectedRatingLevel?.identifier

            FilterChip(
                selected = selected,
                onClick = { onRatingLevelSelect(ratingLevel.takeUnless { selected }) },
                label = { Text(ratingLevel.reaction, style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
}
