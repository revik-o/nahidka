package org.orev.nahidka.ui.goal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.models.GoalEntity

enum class GoalSortField { TITLE, PROGRESS, DEADLINE }

@Composable
fun GoalsTable(
    goals: List<GoalEntity>,
    onGoalClick: (GoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortField by remember { mutableStateOf(GoalSortField.TITLE) }
    var sortAscending by remember { mutableStateOf(true) }

    val sortedGoals = remember(goals, sortField, sortAscending) {
        val comparator = when (sortField) {
            GoalSortField.TITLE -> compareBy<GoalEntity> { it.title }
            GoalSortField.PROGRESS -> compareBy { it.progress }
            GoalSortField.DEADLINE -> compareBy { it.deadline }
        }
        if (sortAscending) goals.sortedWith(comparator) else goals.sortedWith(comparator.reversed())
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GoalHeaderCell("Title", GoalSortField.TITLE, sortField, sortAscending) { 
                if (sortField == GoalSortField.TITLE) sortAscending = !sortAscending else { sortField = GoalSortField.TITLE; sortAscending = true }
            }
            GoalHeaderCell("Progress", GoalSortField.PROGRESS, sortField, sortAscending) { 
                if (sortField == GoalSortField.PROGRESS) sortAscending = !sortAscending else { sortField = GoalSortField.PROGRESS; sortAscending = true }
            }
            GoalHeaderCell("Deadline", GoalSortField.DEADLINE, sortField, sortAscending) { 
                if (sortField == GoalSortField.DEADLINE) sortAscending = !sortAscending else { sortField = GoalSortField.DEADLINE; sortAscending = true }
            }
        }
        Divider()
        
        LazyColumn {
            items(sortedGoals, key = { it.id }) { goal ->
                GoalLinearItem(
                    goal = goal,
                    onClick = onGoalClick
                )
                Divider()
            }
        }
    }
}

@Composable
private fun RowScope.GoalHeaderCell(
    text: String,
    field: GoalSortField,
    currentSortField: GoalSortField,
    sortAscending: Boolean,
    onClick: () -> Unit
) {
    val sortIcon = if (currentSortField == field) {
        if (sortAscending) " ▲" else " ▼"
    } else ""
    
    Text(
        text = text + sortIcon,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
    )
}
