package org.orev.nahidka.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.tasks.generated.resources.Res
import nahidka.shared.ui.tasks.generated.resources.tasks_create
import nahidka.shared.ui.tasks.generated.resources.tasks_rating_levels
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.AddButton
import org.orev.nahidka.ui.common.component.ChoiceSelector
import org.orev.nahidka.ui.common.component.MenuAction
import org.orev.nahidka.ui.common.component.MoreActionsMenu
import org.orev.nahidka.ui.common.layout.LayoutWidth

@Composable
internal fun TasksHeader(
    selectedView: TasksView,
    layoutWidth: LayoutWidth,
    onViewSelect: (TasksView) -> Unit,
    onTaskCreate: () -> Unit,
    onRatingLevelsEdit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChoiceSelector(
            options = TasksView.entries,
            selectedOption = selectedView,
            optionTitle = { tasksView -> stringResource(tasksView.title) },
            onOptionSelect = onViewSelect,
            modifier = if (layoutWidth == LayoutWidth.COMPACT) Modifier.weight(1f) else Modifier,
        )
        AddButton(stringResource(Res.string.tasks_create), layoutWidth, onTaskCreate)
        MoreActionsMenu(listOf(MenuAction(stringResource(Res.string.tasks_rating_levels), onRatingLevelsEdit)))
    }
}
