package org.orev.nahidka

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.*
import org.orev.nahidka.ui.goal.GoalsTable
import org.orev.nahidka.ui.socialbattery.SocialBatteryWidget

@Composable
internal fun PersonalFeaturesScreen(
    feature: PersonalFeature,
    goals: List<GoalRecord>,
    batteryPercentage: Int?,
    onCreateGoal: (String) -> Unit,
    onUpdateGoal: (GoalRecord) -> Unit,
    onUpdateBattery: (Int) -> Unit,
    errorMessage: String?,
) {
    var newTitle by remember(feature) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (feature != PersonalFeature.SOCIAL_BATTERY) {
            Text("Goals", style = MaterialTheme.typography.headlineMedium)
            Row {
                OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f))
                TextButton(enabled = newTitle.isNotBlank(), onClick = {
                    onCreateGoal(newTitle.trim())
                    newTitle = ""
                }) { Text("Add") }
            }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (feature) {
            PersonalFeature.GOALS -> {
                Text("Select a goal to advance its progress by 10%.")
                GoalsTable(goals, onGoalClick = onUpdateGoal)
            }
            PersonalFeature.SOCIAL_BATTERY -> {
                if (batteryPercentage == null) Text("Choose your current social battery level.")
                else SocialBatteryWidget(batteryPercentage / 100f)
                Slider(
                    value = (batteryPercentage ?: 50).toFloat(),
                    onValueChange = { onUpdateBattery(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 99,
                )
            }
        }
    }
}
