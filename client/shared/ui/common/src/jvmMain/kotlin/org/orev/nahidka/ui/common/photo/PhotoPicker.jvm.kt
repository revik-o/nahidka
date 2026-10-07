package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_action_choose_photo
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

private val PHOTO_FILE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val photoPickerTitle = stringResource(Res.string.common_action_choose_photo)
    val coroutineScope = rememberCoroutineScope()

    return {
        choosePhotoFile(photoPickerTitle)?.let { photoFile ->
            coroutineScope.launch {
                onPhotoPick(withContext(Dispatchers.IO) { photoFile.readBytes() })
            }
        }
    }
}

private fun choosePhotoFile(photoPickerTitle: String): File? =
    FileDialog(null as Frame?, photoPickerTitle, FileDialog.LOAD)
        .apply {
            isMultipleMode = false
            filenameFilter = FilenameFilter { _, fileName ->
                fileName
                    .substringAfterLast('.')
                    .lowercase() in PHOTO_FILE_EXTENSIONS
            }
            isVisible = true
        }
        .files
        .firstOrNull()
