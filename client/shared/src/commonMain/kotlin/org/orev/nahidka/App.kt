package org.orev.nahidka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaBackground
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.dashboard.DashboardScreen

@Composable
@Preview
fun App(titleBar: (@Composable () -> Unit)? = null, onFirstFrame: (() -> Unit)? = null) {
    NahidkaTheme {
        Column(Modifier.background(NahidkaBackground).fillMaxSize()) {
            titleBar?.invoke()
            Box(Modifier.safeContentPadding().fillMaxSize().onFirstFrame(onFirstFrame)) {
                DashboardScreen()
            }
        }
    }
}
