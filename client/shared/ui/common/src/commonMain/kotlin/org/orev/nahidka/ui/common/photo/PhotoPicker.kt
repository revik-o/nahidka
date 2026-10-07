package org.orev.nahidka.ui.common.photo

import androidx.compose.runtime.Composable

@Composable
expect fun rememberPhotoPicker(onPhotoPick: (ByteArray) -> Unit): () -> Unit
