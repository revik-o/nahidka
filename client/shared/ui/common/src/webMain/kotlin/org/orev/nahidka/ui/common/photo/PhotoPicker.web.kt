package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import js.buffer.toByteArray
import kotlinx.coroutines.launch
import web.blob.arrayBuffer
import web.dom.document
import web.events.EventHandler
import web.events.addHandler
import web.html.HtmlTagName
import web.html.InputType
import web.html.changeEvent
import web.html.file

private const val PHOTO_MEDIA_TYPES = "image/png,image/jpeg,image/webp,image/gif,image/bmp"

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val coroutineScope = rememberCoroutineScope()

    return {
        val photoInput = document.createElement(HtmlTagName.input)
        photoInput.type = InputType.file
        photoInput.accept = PHOTO_MEDIA_TYPES
        photoInput.changeEvent.addHandler(
            EventHandler {
                photoInput.files
                    ?.item(0)
                    ?.let { photoFile ->
                        coroutineScope.launch {
                            onPhotoPick(
                                photoFile
                                    .arrayBuffer()
                                    .toByteArray(),
                            )
                        }
                    }
            },
        )
        photoInput.click()
    }
}
