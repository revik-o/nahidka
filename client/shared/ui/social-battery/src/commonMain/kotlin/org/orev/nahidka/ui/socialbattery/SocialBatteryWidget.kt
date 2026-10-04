package org.orev.nahidka.ui.socialbattery

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SocialBatteryWidget(batteryLevel: Float, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Social Battery", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(
            progress = { batteryLevel },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
        )
        Text("${(batteryLevel * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
}
