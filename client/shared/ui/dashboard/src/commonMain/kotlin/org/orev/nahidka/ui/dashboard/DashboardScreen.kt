package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.common.ResponsiveLayout
import org.orev.nahidka.ui.socialbattery.SocialBatteryWidget

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    ResponsiveLayout(
        mobileContent = {
            Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
                Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                SocialBatteryWidget(batteryLevel = 0.75f)
                // Additional mobile layout items
            }
        },
        desktopContent = {
            Row(modifier = modifier.fillMaxSize().padding(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    SocialBatteryWidget(batteryLevel = 0.75f)
                }
                Spacer(modifier = Modifier.width(24.dp))
                Column(modifier = Modifier.weight(2f)) {
                    // Desktop specific layout items (e.g. quick tasks, notifications)
                    Text("Quick Overview", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    )
}
