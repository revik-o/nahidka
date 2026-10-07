package org.orev.nahidka.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import java.awt.Color as AwtColor
import org.orev.nahidka.DesktopApplication
import org.orev.nahidka.DesktopStartupTrace
import org.orev.nahidka.ui.common.theme.NahidkaBackground

private val WINDOW_BACKGROUND = AwtColor(NahidkaBackground.toArgb())

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NahidkaWindow(
    onCloseRequest: () -> Unit,
    desktopStartupTrace: DesktopStartupTrace,
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
        transparent = host.transparentWindow,
        resizable = true,
        init = { window ->
            window.background = WINDOW_BACKGROUND
            window.contentPane.background = WINDOW_BACKGROUND
            window.minimumSize = Dimension(360, 480)
            host.prepare(window)
        },
    ) {
        val chrome = host.chrome
        Box(Modifier.fillMaxSize().background(if (host.transparentWindow) Color.Transparent else NahidkaBackground)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(chrome.contentInset)
                    .clip(chrome.contentShape)
                    .background(NahidkaBackground),
            ) {
                NahidkaTitleBar(controller, chrome)
                DesktopApplication(desktopStartupTrace)
            }
            chrome.FrameOverlay()
        }
    }
}
