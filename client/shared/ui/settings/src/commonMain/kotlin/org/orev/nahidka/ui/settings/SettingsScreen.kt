package org.orev.nahidka.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.orev.nahidka.ui.common.theme.NahidkaTheme

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var darkModeEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Enable Notifications", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
        }
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    NahidkaTheme {
        SettingsScreen()
    }
}
