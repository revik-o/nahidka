package org.orev.nahidka

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import kotlinx.coroutines.CompletableDeferred

@Composable
fun Modifier.onFirstFrame(onFirstFrame: (() -> Unit)?): Modifier {
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
