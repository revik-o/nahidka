package org.orev.nahidka

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
internal fun DesktopApplication(desktopStartupTrace: DesktopStartupTrace) {
    var applicationVisible by remember { mutableStateOf(false) }

    if (applicationVisible) {
        App(onFirstFrame = desktopStartupTrace::onDashboardFrame)
    } else {
        ApplicationSplash(
            Modifier.onFirstFrame {
                desktopStartupTrace.onSplashFrame()
                applicationVisible = true
            },
        )
    }
}
