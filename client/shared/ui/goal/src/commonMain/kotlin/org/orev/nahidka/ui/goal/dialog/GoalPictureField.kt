package org.orev.nahidka.ui.goal.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.common.generated.resources.common_action_choose_photo
import nahidka.shared.ui.goal.generated.resources.Res
import nahidka.shared.ui.goal.generated.resources.goal_action_remove_picture
import nahidka.shared.ui.goal.generated.resources.goal_field_picture
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.goals.dto.GoalPicture
import org.orev.nahidka.ui.common.component.ChoiceChips
import org.orev.nahidka.ui.common.component.LabeledField
import org.orev.nahidka.ui.common.photo.rememberPhotoPicker
import org.orev.nahidka.ui.goal.component.GOAL_EMOJI_PICTURES
import org.orev.nahidka.ui.goal.component.GoalPictureView
import org.orev.nahidka.ui.goal.model.GoalDraft
import nahidka.shared.ui.common.generated.resources.Res as CommonResources

private val GOAL_EDITOR_PICTURE_SIZE = 72.dp

@Composable
internal fun GoalPictureField(goalDraft: GoalDraft, onGoalDraftEdit: ((GoalDraft) -> GoalDraft) -> Unit) {
    val openPhotoPicker = rememberPhotoPicker { photoContent ->
        onGoalDraftEdit { draft -> draft.copy(picture = GoalPicture.Photo(photoContent)) }
    }

    LabeledField(stringResource(Res.string.goal_field_picture)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoalPictureView(goalDraft.picture, goalDraft.title, GOAL_EDITOR_PICTURE_SIZE)
            Column {
                TextButton(onClick = openPhotoPicker) {
                    Text(stringResource(CommonResources.string.common_action_choose_photo))
                }
                if (goalDraft.picture != null) {
                    TextButton(onClick = { onGoalDraftEdit { draft -> draft.copy(picture = null) } }) {
                        Text(stringResource(Res.string.goal_action_remove_picture))
                    }
                }
            }
        }
        ChoiceChips(
            options = GOAL_EMOJI_PICTURES,
            selectedOption = goalDraft.picture as? GoalPicture.Emoji,
            optionTitle = { emojiPicture -> emojiPicture.symbol },
            onOptionSelect = { emojiPicture -> onGoalDraftEdit { draft -> draft.copy(picture = emojiPicture) } },
        )
    }
}
