package org.orev.nahidka.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.settings.dto.*

@Composable
fun SettingsScreen(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsTheme.entries.forEach { theme ->
                FilterChip(
                    selected = settings.theme == theme,
                    onClick = { onSettingsChange(settings.copy(theme = theme)) },
                    label = { Text(theme.name.lowercase().replace('_', ' ')) },
                )
            }
        }
        Text("Language", style = MaterialTheme.typography.titleMedium)
        Column {
            SettingsLanguage.entries.forEach { language ->
                FilterChip(
                    selected = settings.language == language,
                    onClick = { onSettingsChange(settings.copy(language = language)) },
                    label = { Text(language.name.lowercase().replace('_', ' ')) },
                )
            }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
