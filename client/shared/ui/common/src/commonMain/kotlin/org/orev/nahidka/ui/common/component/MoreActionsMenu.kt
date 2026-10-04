package org.orev.nahidka.ui.common.component

import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_more
import org.jetbrains.compose.resources.stringResource

@Composable
fun MoreActionsMenu(menuActions: List<MenuAction>) {
    val moreActionsTitle = stringResource(Res.string.common_action_more)

    ActionsMenu(menuActions) { onMenuOpen ->
        IconButton(onClick = onMenuOpen, modifier = Modifier.semantics { contentDescription = moreActionsTitle }) {
            Text("⋮")
        }
    }
}
