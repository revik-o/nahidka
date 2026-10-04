package org.orev.nahidka

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.feature.goals.dto.*
import org.orev.nahidka.feature.tasks.dto.*
import org.orev.nahidka.ui.goal.GoalsTable
import org.orev.nahidka.ui.socialbattery.SocialBatteryWidget
import org.orev.nahidka.ui.task.TasksTable

@Composable
internal fun PersonalFeaturesScreen(
    feature: PersonalFeature,
    tasks: List<TaskRecord>,
    goals: List<GoalRecord>,
    batteryPercentage: Int?,
    onCreateTask: (String) -> Unit,
    onUpdateTask: (TaskRecord) -> Unit,
    onCreateGoal: (String) -> Unit,
    onUpdateGoal: (GoalRecord) -> Unit,
    onUpdateBattery: (Int) -> Unit,
    errorMessage: String?,
) {
    var newTitle by remember(feature) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (feature != PersonalFeature.SOCIAL_BATTERY) {
            Text(if (feature == PersonalFeature.TASKS) "Tasks" else "Goals", style = MaterialTheme.typography.headlineMedium)
            Row {
                OutlinedTextField(newTitle, { newTitle = it }, label = { Text("Title") }, modifier = Modifier.weight(1f))
                TextButton(enabled = newTitle.isNotBlank(), onClick = {
                    if (feature == PersonalFeature.TASKS) onCreateTask(newTitle.trim()) else onCreateGoal(newTitle.trim())
                    newTitle = ""
                }) { Text("Add") }
            }
        }
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when (feature) {
            PersonalFeature.TASKS -> {
                Text("Select a task to advance its status.")
                TasksTable(tasks, onTaskClick = onUpdateTask)
            }
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
