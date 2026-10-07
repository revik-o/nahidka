package org.orev.nahidka.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nahidka.shared.ui.common.generated.resources.Res
import nahidka.shared.ui.common.generated.resources.common_error_unsaved
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.settings.dto.*
import org.orev.nahidka.ui.common.layout.LayoutWidth

@Composable
fun SettingsScreen(settingsViewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val settingsSnapshot by settingsViewModel.settingsState.collectAsStateWithLifecycle()
    val saveFailed by settingsViewModel.saveFailed.collectAsStateWithLifecycle()
    val settings = settingsSnapshot.settings

    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LayoutWidth.of(maxWidth).screenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { settingsViewModel.saveSettings(settings.copy(theme = theme)) },
                        label = { Text(theme.name.lowercase().replace('_', ' ')) },
                    )
                }
            }
            Text("Language", style = MaterialTheme.typography.titleMedium)
            Column {
                SettingsLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = settings.language == language,
                        onClick = { settingsViewModel.saveSettings(settings.copy(language = language)) },
                        label = { Text(language.name.lowercase().replace('_', ' ')) },
                    )
                }
            }
            if (saveFailed) {
                Text(stringResource(Res.string.common_error_unsaved), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
