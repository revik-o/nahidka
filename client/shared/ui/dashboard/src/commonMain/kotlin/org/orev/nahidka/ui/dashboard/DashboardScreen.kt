package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.orev.nahidka.ui.common.theme.NahidkaTheme

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, viewModel: DashboardViewModel = viewModel {
    DashboardViewModel()
}
) {
    val state by viewModel.state.collectAsState()
    var detail by remember {
        mutableStateOf<String?>(null)
    }
    var action by remember {
        mutableStateOf<String?>(null)
    }
    var noteVisible by remember {
        mutableStateOf(true)
    }
    NahidkaTheme {
        BoxWithConstraints(modifier.fillMaxSize().background(Ink)) {
            val desktop = maxWidth >= 1000.dp
            val wide = maxWidth >= 1380.dp
            Column {
                Row(Modifier.weight(1f)) {
                    if (desktop) Sidebar {
                        detail = it
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(if (desktop) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (!desktop) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Brand(Modifier.weight(1f))
                            TextButton(onClick = {
                                detail = "Recent Activity"
                            }
                            ) {
                                Text("♧", fontSize = 26.sp, color = Muted)
                            }
                        }
                        Hero(desktop, onAction = {
                            action = "Quick Action"
                        }
                        )
                        Text("SAMPLE DASHBOARD · Changes are saved for this session", color = Muted.copy(alpha = .65f), fontSize = 10.sp)
                        BoxWithConstraints {
                            val columns = if (maxWidth >= 560.dp) 4 else 2
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                (0..3).toList().chunked(columns).forEach {
                                    indices ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        indices.forEach {
                                            index -> Summary(index, state, Modifier.weight(1f)) {
                                                detail = listOf("Relationship Score", "Social Battery", "Promise Tracker", "Calendar")[index]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (desktop) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                BatteryCard(state, Modifier.weight(1f)) {
                                    detail = "Social Battery"
                                }
                                EventsCard(Modifier.weight(1f)) {
                                    detail = "Calendar"
                                }
                                PromisesCard(state, Modifier.weight(1f), {
                                    viewModel.handleEvent(DashboardEvent.TogglePromise(it))
                                }
                                ) {
                                    detail = "Promise Tracker"
                                }
                            }
                        }
                        else {
                            EventsCard(Modifier.fillMaxWidth()) {
                                detail = "Calendar"
                            }
                            AdaptivePair({
                                m -> BatteryCard(state, m) {
                                    detail = "Social Battery"
                                }
                            }
                            , {
                                m -> PromisesCard(state, m, {
                                    viewModel.handleEvent(DashboardEvent.TogglePromise(it))
                                }
                                ) {
                                    detail = "Promise Tracker"
                                }
                            }
                            )
                        }
                        if (desktop) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            FinanceCard(Modifier.weight(1f)) {
                                detail = "Finance Manager"
                            }
                            SpendingCard(Modifier.weight(1.1f))
                            SavingsCard(Modifier.weight(.9f)) {
                                detail = "Shared Goals"
                            }
                        }
                        else AdaptivePair({
                            m -> FinanceCard(m) {
                                detail = "Finance Manager"
                            }
                        }
                        , {
                            m -> SavingsCard(m) {
                                detail = "Shared Goals"
                            }
                        }
                        )
                        if (!wide) ActivityCard(state) {
                            detail = "Recent Activity"
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (wide) Column(Modifier.width(260.dp).fillMaxHeight().border(1.dp, Edge).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Spacer(Modifier.height(28.dp))
                        ActivityCard(state) {
                            detail = "Recent Activity"
                        }
                        QuickActions {
                            action = it
                        }
                        if (noteVisible) Tile {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Label("💕 Love Note", Modifier.weight(1f))
                                TextButton(onClick = {
                                    noteVisible = false
                                }
                                ) {
                                    Text("×", color = Muted)
                                }
                            }
                            Text(state.loveNote, color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
                if (!desktop) Row(Modifier.fillMaxWidth().background(Panel).border(1.dp, Edge).navigationBarsPadding().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                    listOf("⌂" to "Home", "ϟ" to "Battery", "+" to "", "✓" to "Promises", "▣" to "Finance").forEachIndexed {
                        i, (icon, name) ->
                        Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable {
                            when (i) {
                                0 -> Unit
                                2 -> action = "Quick Action"
                                else -> detail = listOf("", "Social Battery", "", "Promise Tracker", "Finance Manager")[i]
                            }
                        }
                        .padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(icon, color = if (i == 0 || i == 2) Purple else Muted, fontSize = if (i == 2) 36.sp else 24.sp)
                            if (name.isNotEmpty()) Text(name, color = if (i == 0) Purple else Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        detail?.let {
            title ->
            AlertDialog(onDismissRequest = {
                detail = null
            }
            , title = {
                Text(title)
            }
            , text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (title) {
                        "Social Battery" -> {
                            Text("How are you feeling right now?")
                            Slider(value = state.socialBatteryLevel, onValueChange = {
                                viewModel.handleEvent(DashboardEvent.SetBattery(it))
                            }
                            )
                            Text("Your battery: ${(state.socialBatteryLevel * 100).toInt()}% · Partner: 85%")
                        }
                        "Promise Tracker" -> state.promises.forEach {
                            promise -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(promise.completed, {
                                    viewModel.handleEvent(DashboardEvent.TogglePromise(promise.id))
                                }
                                )
                                Text(promise.title)
                            }
                        }
                        "Calendar" -> eventRows.forEach {
                            Text("${it[1]}\n${it[2]} · ${it[3]}")
                        }
                        "Finance Manager" -> Text("This month\n\nSpent: $3,428.75\nIncome: $6,250.00\nLeft to spend: $2,821.25\n\nHome  $1,245\nFood  $642\nFun  $512\nOther  $1,029.75")
                        "Shared Goals" -> Text("New Camera\n\n$1,440 saved of $2,000 · 72%\n$560 remaining")
                        "Relationship Score" -> Text("92% — Amazing!\n\nA sample overview of your shared time, promises, and connection.")
                        "Recent Activity" -> state.activity.forEach {
                            Text(it)
                        }
                        "Notes" -> Text(state.loveNote)
                        else -> Text("$title is not connected to the dashboard yet. Your existing feature pages are preserved.")
                    }
                }
            }
            , confirmButton = {
                TextButton(onClick = {
                    detail = null
                }
                ) {
                    Text("Done")
                }
            }
            )
        }
        action?.let {
            title ->
            var input by remember(title) {
                mutableStateOf("")
            }
            AlertDialog(onDismissRequest = {
                action = null
            }
            , title = {
                Text(title)
            }
            , text = {
                if (title == "Quick Action") Column {
                    listOf("Add Promise", "Add Expense", "New Note", "Add Memory").forEach {
                        option -> TextButton(onClick = {
                            action = option
                        }
                        ) {
                            Text(option)
                        }
                    }
                }
                else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add to your sample dashboard. This stays in the current session.", color = Muted)
                    OutlinedTextField(input, {
                        input = it
                    }
                    , label = {
                        Text(if (title == "Add Expense") "Expense description" else "Write something")
                    }
                    , maxLines = 4)
                }
            }
            , confirmButton = {
                if (title != "Quick Action") TextButton(enabled = input.isNotBlank(), onClick = {
                    viewModel.handleEvent(DashboardEvent.AddEntry(title, input.trim()))
                    action = null
                }
                ) {
                    Text("Add")
                }
            }
            , dismissButton = {
                TextButton(onClick = {
                    action = null
                }
                ) {
                    Text("Cancel")
                }
            }
            )
        }
    }
}
