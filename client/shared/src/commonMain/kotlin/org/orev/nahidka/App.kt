package org.orev.nahidka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.orev.nahidka.ui.dashboard.DashboardScreen
import org.orev.nahidka.ui.navigation.DashboardRoute


@Composable
@Preview
fun App(titleBar: (@Composable () -> Unit)? = null) {
    MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF090913))) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            titleBar?.invoke()
            Box(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
            ) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = DashboardRoute
                ) {
                    composable<DashboardRoute> {
                        DashboardScreen()
                    }
                }
            }
        }
    }
}
