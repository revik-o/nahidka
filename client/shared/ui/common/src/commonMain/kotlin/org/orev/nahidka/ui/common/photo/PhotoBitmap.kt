package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.roundToInt

private const val PHOTO_BITMAP_MAXIMUM_SIDE = 512

@Composable
fun rememberPhotoBitmap(photoContent: ByteArray): ImageBitmap? =
    produceState<ImageBitmap?>(null, photoContent) {
        value = withContext(Dispatchers.Default) { decodePhotoBitmap(photoContent) }
    }.value

private fun decodePhotoBitmap(photoContent: ByteArray): ImageBitmap? =
    runCatching { photoContent.decodeToImageBitmap() }
        .getOrNull()
        ?.scaledDown()

private fun ImageBitmap.scaledDown(): ImageBitmap {
    val scale = PHOTO_BITMAP_MAXIMUM_SIDE.toFloat() / maxOf(width, height)

    if (scale >= 1f) {
        return this
    }

    val scaledSize = IntSize(
        width = (width * scale)
            .roundToInt()
            .coerceAtLeast(1),
        height = (height * scale)
            .roundToInt()
            .coerceAtLeast(1),
    )
    val scaledBitmap = ImageBitmap(scaledSize.width, scaledSize.height)

    Canvas(scaledBitmap).drawImageRect(
        image = this,
        dstSize = scaledSize,
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )

    return scaledBitmap
}
