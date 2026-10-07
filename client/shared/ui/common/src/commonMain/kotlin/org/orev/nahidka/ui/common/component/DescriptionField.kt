package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_field_description
import org.jetbrains.compose.resources.stringResource

private const val DESCRIPTION_MINIMUM_LINES = 3
private const val DESCRIPTION_MAXIMUM_LINES = 6

@Composable
fun DescriptionField(description: String, onDescriptionChange: (String) -> Unit) {
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(Res.string.common_field_description)) },
        minLines = DESCRIPTION_MINIMUM_LINES,
        maxLines = DESCRIPTION_MAXIMUM_LINES,
    )
}
