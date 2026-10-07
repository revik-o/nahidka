package org.orev.nahidka.ui.tasks.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.orev.nahidka.feature.tasks.dto.TaskRatingLevel
import org.orev.nahidka.ui.common.component.ChoiceChips

@Composable
internal fun TaskRatingReactions(
    ratingLevels: List<TaskRatingLevel>,
    selectedRatingLevel: TaskRatingLevel?,
    onRatingLevelSelect: (TaskRatingLevel?) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceChips(
        options = ratingLevels,
        selectedOption = selectedRatingLevel,
        optionTitle = { ratingLevel -> ratingLevel.reaction },
        onOptionSelect = onRatingLevelSelect,
        modifier = modifier,
    )
}
