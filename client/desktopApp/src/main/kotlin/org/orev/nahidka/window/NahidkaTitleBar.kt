package org.orev.nahidka.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.orev.nahidka.shell.ApplicationTopBar
import org.orev.nahidka.shell.ApplicationTopBarState

internal val WINDOW_TITLE_BAR_HEIGHT = 40.dp

@Composable
internal fun NahidkaTitleBar(
    desktopWindowController: DesktopWindowController,
    windowChrome: WindowChrome,
    applicationTopBarState: ApplicationTopBarState,
    applicationTopBarActions: @Composable () -> Unit,
) {
    SideEffect { windowChrome.refreshState() }
    val windowDensity = LocalDensity.current.density
    val titleBarColorScheme = MaterialTheme.colorScheme
    val titleBarContentColor = titleBarColorScheme.onBackground.copy(alpha = if (windowChrome.isActive) 1f else 0.6f)

    CompositionLocalProvider(LocalContentColor provides titleBarContentColor) {
        ApplicationTopBar(
            applicationTopBarState = applicationTopBarState,
            modifier = Modifier
                .height(WINDOW_TITLE_BAR_HEIGHT)
                .background(titleBarColorScheme.background)
                .onGloballyPositioned { titleBarCoordinates ->
                    windowChrome.updateHeader(titleBarCoordinates.boundsInWindow(), windowDensity)
                }
                .padding(start = windowChrome.leftInset, end = windowChrome.rightInset),
            captionContainer = { captionModifier, captionContent ->
                windowChrome.Caption(captionModifier, captionContent)
            },
        ) {
            applicationTopBarActions()
            if (!windowChrome.controlsAreNative) {
                WindowControlButton(
                    windowControl = WindowControl.Minimize,
                    accessibilityLabel = "Minimize",
                    showRestoreIcon = false,
                    windowChrome = windowChrome,
                    onControlClick = desktopWindowController::minimize,
                )
                WindowControlButton(
                    windowControl = WindowControl.Maximize,
                    accessibilityLabel = if (desktopWindowController.isMaximized) "Restore" else "Maximize",
                    showRestoreIcon = desktopWindowController.isMaximized,
                    windowChrome = windowChrome,
                    onControlClick = desktopWindowController::toggleMaximize,
                )
                WindowControlButton(
                    windowControl = WindowControl.Close,
                    accessibilityLabel = "Close",
                    showRestoreIcon = false,
                    windowChrome = windowChrome,
                    onControlClick = desktopWindowController::close,
                )
            }
        }
    }
}
