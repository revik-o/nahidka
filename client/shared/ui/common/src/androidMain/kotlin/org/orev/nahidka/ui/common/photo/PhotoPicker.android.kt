package org.orev.nahidka.ui.common.photo

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val contentResolver = LocalContext.current.contentResolver
    val coroutineScope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { photoUri ->
        photoUri?.let { pickedPhotoUri ->
            coroutineScope.launch {
                contentResolver
                    .readPhoto(pickedPhotoUri)
                    ?.let(onPhotoPick)
            }
        }
    }

    return {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private suspend fun ContentResolver.readPhoto(photoUri: Uri): ByteArray? =
    withContext(Dispatchers.IO) {
        openInputStream(photoUri)?.use(InputStream::readBytes)
    }
