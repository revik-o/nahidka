package org.orev.nahidka

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.orev.nahidka.di.populateDemoData
import org.orev.nahidka.di.rememberApplicationSession
import org.orev.nahidka.di.sessionViewModel
import org.orev.nahidka.feature.settings.service.SettingsLocalDataSource
import org.orev.nahidka.navigation.ApplicationNavigator
import org.orev.nahidka.settings.rememberSettingsLocalDataSource
import org.orev.nahidka.shell.ApplicationShell
import org.orev.nahidka.shell.ApplicationTopBar
import org.orev.nahidka.shell.ApplicationTopBarRenderer
import org.orev.nahidka.shell.DestinationContent
import org.orev.nahidka.ui.common.theme.NahidkaTheme
import org.orev.nahidka.ui.financialmanagement.mock.FinancialDemoData
import org.orev.nahidka.ui.settings.isDarkTheme

@Preview
@Composable
fun App(
    onFirstFrame: (() -> Unit)? = null,
    settingsLocalDataSource: SettingsLocalDataSource = rememberSettingsLocalDataSource(),
    applicationTopBar: ApplicationTopBarRenderer = { applicationTopBarState, applicationTopBarActions ->
        ApplicationTopBar(applicationTopBarState, actions = applicationTopBarActions)
    },
    navigationSidebarTopInset: Dp = 0.dp,
) {
    val applicationSessionOwner = rememberApplicationSession(FinancialDemoData.sessionConfig, settingsLocalDataSource)
    val settingsSnapshot by applicationSessionOwner
        .sessionViewModel { settingsViewModel }
        .settingsState
        .collectAsStateWithLifecycle()
    val applicationNavigator = remember(applicationSessionOwner) { ApplicationNavigator() }

    LaunchedEffect(applicationSessionOwner) {
        populateDemoData(applicationSessionOwner.graph)
    }

    NahidkaTheme(darkTheme = settingsSnapshot.settings.theme.isDarkTheme()) {
        ApplicationShell(
            applicationNavigator = applicationNavigator,
            notificationsViewModel = applicationSessionOwner.sessionViewModel { notificationsViewModel },
            applicationTopBar = applicationTopBar,
            navigationSidebarTopInset = navigationSidebarTopInset,
            modifier = Modifier.onFirstFrame(onFirstFrame),
        ) { destination ->
            DestinationContent(destination, applicationSessionOwner, applicationNavigator)
        }
    }
}
