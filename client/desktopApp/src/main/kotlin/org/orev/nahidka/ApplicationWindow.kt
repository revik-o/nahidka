package org.orev.nahidka

import androidx.compose.runtime.Composable

@Composable
internal fun ApplicationWindow(onCloseRequest: () -> Unit) {
    ApplicationWindowDecoration(onCloseRequest) {

    }
}