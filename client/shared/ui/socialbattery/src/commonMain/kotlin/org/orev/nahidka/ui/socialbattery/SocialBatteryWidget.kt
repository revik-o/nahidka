package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SocialBatteryWidget(
    batteryLevel: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Social Battery", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { batteryLevel },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = when {
                batteryLevel > 0.6f -> MaterialTheme.colorScheme.primary
                batteryLevel > 0.3f -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(batteryLevel * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
