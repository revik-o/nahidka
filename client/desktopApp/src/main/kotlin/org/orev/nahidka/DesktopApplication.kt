package org.orev.nahidka

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.orev.nahidka.shell.ApplicationTopBarRenderer
import org.orev.nahidka.shell.ApplicationTopBarState
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.common.theme.NahidkaTheme

@Composable
internal fun DesktopApplication(
    desktopStartupTrace: DesktopStartupTrace,
    applicationTopBar: ApplicationTopBarRenderer,
    navigationSidebarTopInset: Dp,
) {
    var applicationVisible by remember { mutableStateOf(false) }

    if (applicationVisible) {
        App(
            onFirstFrame = desktopStartupTrace::onDashboardFrame,
            applicationTopBar = applicationTopBar,
            navigationSidebarTopInset = navigationSidebarTopInset,
        )
    } else {
        NahidkaTheme(darkTheme = true) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val splashLayoutWidth = LayoutWidth.of(maxWidth)
                Column {
                    applicationTopBar(
                        ApplicationTopBarState(title = "Nahidka", layoutWidth = splashLayoutWidth),
                    ) {}
                    ApplicationSplash(
                        Modifier.onFirstFrame {
                            desktopStartupTrace.onSplashFrame()
                            applicationVisible = true
                        },
                    )
                }
            }
        }
    }
}
