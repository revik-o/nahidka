package org.orev.nahidka.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.rememberWindowState
import java.awt.Color
import java.awt.Dimension
import org.orev.nahidka.App
import org.orev.nahidka.ui.common.theme.NahidkaBackground

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NahidkaWindow(
    onCloseRequest: () -> Unit,
    onFirstFrame: (() -> Unit)? = null,
) {
    val platform = remember { DesktopPlatform.current() }
    val state = rememberWindowState()
    val closeRequest = rememberUpdatedState(onCloseRequest)
    val controller = remember(state) {
        DesktopWindowController(state) { closeRequest.value() }
    }
    val host = remember(platform, controller) {
        WindowChromeHost(windowChromeFactory(platform), controller)
    }
    DisposableEffect(host) { onDispose { host.close() } }

    SwingWindow(
        onCloseRequest = controller::close,
        state = state,
        title = "nahidka",
        decoration = if (platform == DesktopPlatform.Linux) {
            WindowDecoration.Undecorated(resizerThickness = 0.dp)
        } else {
            WindowDecoration.SystemDefault
        },
        transparent = false,
        resizable = true,
        init = { window ->
            window.background = Color(0x09, 0x09, 0x13)
            window.contentPane.background = Color(0x09, 0x09, 0x13)
            window.minimumSize = Dimension(360, 480)
            host.prepare(window)
        },
    ) {
        val chrome = host.chrome
        Box(Modifier.fillMaxSize().background(NahidkaBackground)) {
            Box(Modifier.fillMaxSize().padding(chrome.contentInset)) {
                App(
                    titleBar = { NahidkaTitleBar(controller, chrome) },
                    onFirstFrame = onFirstFrame,
                )
            }
            chrome.FrameOverlay()
        }
    }
}
