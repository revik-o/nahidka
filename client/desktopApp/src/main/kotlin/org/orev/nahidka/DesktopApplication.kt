package org.orev.nahidka

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.orev.nahidka.shell.ApplicationTopBarRenderer
import org.orev.nahidka.shell.ApplicationTopBarState
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.common.theme.NahidkaTheme

@Composable
internal fun DesktopApplication(
    applicationTopBar: ApplicationTopBarRenderer,
    navigationSidebarTopInset: Dp,
) {
    var applicationVisible by remember { mutableStateOf(false) }

    if (applicationVisible) {
        App(
            applicationTopBar = applicationTopBar,
            navigationSidebarTopInset = navigationSidebarTopInset,
        )
    } else {
        NahidkaTheme(darkTheme = true) { // FIXME better move to ApplicationSplash (AI)
            BoxWithConstraints(Modifier.fillMaxSize()) { // FIXME better move to ApplicationSplash (AI)
                val splashLayoutWidth = LayoutWidth.of(maxWidth) // FIXME better move to ApplicationSplash (AI)

                Column { // FIXME better move to ApplicationSplash (AI)
                    applicationTopBar( // FIXME better move to ApplicationSplash (AI)
                        ApplicationTopBarState( // FIXME better move to ApplicationSplash (AI)
                            title = "Nahidka", // FIXME move to constant to class for Application constants which I mentioned before (AI)
                            layoutWidth = splashLayoutWidth
                        ),
                    ) {}
                    ApplicationSplash(
                        Modifier.onFirstFrame {
                            applicationVisible = true
                        },
                    )
                }
            }
        }
    }
}
