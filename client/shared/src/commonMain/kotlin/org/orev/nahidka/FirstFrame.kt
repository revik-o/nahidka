package org.orev.nahidka

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import kotlinx.coroutines.CompletableDeferred

/** Signal only after content has drawn, then let that frame reach the platform renderer. */
@Composable
internal fun Modifier.onFirstFrame(onFirstFrame: (() -> Unit)?): Modifier {
    if (onFirstFrame == null) return this
    val drawn = remember { CompletableDeferred<Unit>() }
    val callback = rememberUpdatedState(onFirstFrame)
    LaunchedEffect(drawn) {
        drawn.await()
        withFrameNanos { }
        callback.value.invoke()
    }
    return drawWithContent {
        drawContent()
        drawn.complete(Unit)
    }
}
