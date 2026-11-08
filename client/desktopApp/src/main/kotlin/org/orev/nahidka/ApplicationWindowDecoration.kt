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
import org.orev.nahidka.shell.ApplicationTopBarState
import org.orev.nahidka.ui.common.layout.LayoutWidth
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.window.NahidkaTitleBar

@Composable
internal fun ApplicationWindowDecoration(onCloseRequest: () -> Unit, content: @Composable () -> Unit) {
    var applicationVisible by remember { mutableStateOf(false) }

    NahidkaTheme(darkTheme = true) { // FIXME better move to ApplicationSplash (AI)
        BoxWithConstraints(Modifier.fillMaxSize()) { // FIXME better move to ApplicationSplash (AI)
            val splashLayoutWidth = LayoutWidth.of(maxWidth) // FIXME better move to ApplicationSplash (AI)

            Column { // FIXME better move to ApplicationSplash (AI)
                /*
                applicationTopBar( // FIXME better move to ApplicationSplash (AI)
                    ApplicationTopBarState( // FIXME better move to ApplicationSplash (AI)
                        title = ApplicationConstants.APPLICATION_TITLE, // FIXME move to constant to class for Application constants which I mentioned before (AI)
                        layoutWidth = splashLayoutWidth
                    ),
                ) {}
                 */
                ApplicationSplash(
                    Modifier.onFirstFrame {
                        applicationVisible = true
                    },
                )
            }
        }
    }
}