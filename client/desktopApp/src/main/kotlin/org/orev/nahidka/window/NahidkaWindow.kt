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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.rememberWindowState
import org.orev.nahidka.DesktopApplication
import org.orev.nahidka.ui.common.theme.NahidkaBackground
import java.awt.Dimension
import java.awt.Color as AwtColor

private val DESKTOP_WINDOW_BACKGROUND_COLOR = AwtColor(NahidkaBackground.toArgb())

@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun NahidkaWindow(
    onCloseRequest: () -> Unit,
) {
    val windowState = rememberWindowState() // TODO think to make custom winstate func!!! (human)
    val desktopPlatform = remember { DesktopPlatform.current() } // FIXME after fixing DesktopPlatform.current() we can remove `desktopPlatform` (AI)
    val latestCloseRequest = rememberUpdatedState(onCloseRequest)
    val desktopWindowController = remember(windowState) {
        DesktopWindowController(windowState) { latestCloseRequest.value() }
    }
    val windowChromeHost = remember(desktopPlatform, desktopWindowController) { // FIXME redesign (human)
        WindowChromeHost(windowChromeFactory(desktopPlatform), desktopWindowController)
    }

    DisposableEffect(windowChromeHost) { onDispose { windowChromeHost.close() } } // TODO DisposableEffect can not automatically use `AutoCloseable` (human and AI)
    SwingWindow(
        onCloseRequest = desktopWindowController::close,
        state = windowState,
        title = "nahidka", // FIXME create class/objet for APPLICATION CONSTANTS and move title to this entity (AI)
        decoration = if (desktopPlatform == DesktopPlatform.Linux) { // FIXME not only for Linux OSs. For Windows same. Also better to do it on MacOS but need to proof that nav buttons may support native tailing features form MacOS (AI)
            WindowDecoration.Undecorated(resizerThickness = 0.dp)
        } else {
            WindowDecoration.SystemDefault
        },
        transparent = windowChromeHost.transparentWindow,
        resizable = true,
        init = { window ->
            window.background = DESKTOP_WINDOW_BACKGROUND_COLOR
            window.contentPane.background = DESKTOP_WINDOW_BACKGROUND_COLOR
            window.minimumSize = Dimension(360, 480) // FIXME also need move numbers to class/objet for APPLICATION CONSTANTS (AI)
            windowChromeHost.prepare(window)
        },
    ) {
        val windowChrome = windowChromeHost.chrome

        Box(
            Modifier
                .fillMaxSize()
                .background(if (windowChromeHost.transparentWindow) Color.Transparent else NahidkaBackground), // FIXME remove `transparentWindow` (AI)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(windowChrome.contentInset)
                    .clip(windowChrome.contentShape)
                    .background(NahidkaBackground),
            ) {
                DesktopApplication(
                    applicationTopBar = { applicationTopBarState, applicationTopBarActions ->
                        NahidkaTitleBar(desktopWindowController, windowChrome, applicationTopBarState, applicationTopBarActions)
                    },
                    navigationSidebarTopInset = if (windowChrome.controlsAreNative) WINDOW_TITLE_BAR_HEIGHT else 0.dp,
                )
            }
            windowChrome.FrameOverlay()
        }
    }
}
