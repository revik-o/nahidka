package org.orev.nahidka.ui.common.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.brand_mark
import org.jetbrains.compose.resources.painterResource

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.brand_mark),
        contentDescription = null,
        modifier = modifier,
    )
}
