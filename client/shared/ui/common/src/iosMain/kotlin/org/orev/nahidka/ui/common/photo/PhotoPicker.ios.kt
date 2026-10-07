package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.PhotosUI.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

private const val PHOTO_JPEG_COMPRESSION_QUALITY = 0.9

@Composable
actual fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit {
    val presentingViewController = LocalUIViewController.current
    val currentOnPhotoPick by rememberUpdatedState(onPhotoPick)
    val photoPickerDelegate = remember { PhotoPickerDelegate { photoContent -> currentOnPhotoPick(photoContent) } }

    return {
        val photoPickerConfiguration = PHPickerConfiguration()
            .apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
        val photoPickerViewController = PHPickerViewController(photoPickerConfiguration)
            .apply { delegate = photoPickerDelegate }

        presentingViewController.presentViewController(photoPickerViewController, animated = true, completion = null)
    }
}

private class PhotoPickerDelegate(
    private val onPhotoPick: (ByteArray) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        (didFinishPicking.firstOrNull() as? PHPickerResult)
            ?.itemProvider
            ?.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { pickedPhotoData, _ ->
                pickedPhotoData
                    ?.let { photoData -> UIImage(data = photoData) }
                    ?.let { pickedPhoto -> UIImageJPEGRepresentation(pickedPhoto, PHOTO_JPEG_COMPRESSION_QUALITY) }
                    ?.toByteArray()
                    ?.let { photoContent ->
                        dispatch_async(dispatch_get_main_queue()) { onPhotoPick(photoContent) }
                    }
            }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).apply {
        usePinned { pinnedPhotoContent -> memcpy(pinnedPhotoContent.addressOf(0), bytes, length) }
    }
