package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenFinance: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier,
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
    BoxWithConstraints(modifier.fillMaxSize().background(Ink)) {
        val desktop = maxWidth >= 1000.dp
        val wide = maxWidth >= 1380.dp
        Column {
            Row(Modifier.weight(1f)) {
                if (desktop) Sidebar {
                    if (it == "Finance Manager") onOpenFinance() else detail = it
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentPadding = PaddingValues(if (desktop) 24.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!desktop) item(key = "brand", contentType = "brand") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Brand(Modifier.weight(1f))
                            TextButton(onClick = { detail = "Recent Activity" }) {
                                Text("♧", fontSize = 26.sp, color = Muted)
                            }
                        }
                    }
                    item(key = "hero", contentType = "hero") {
                        Hero(desktop, onAction = { action = "Quick Action" })
                    }
                    item(key = "sample-notice", contentType = "notice") {
                        Text("SAMPLE DASHBOARD · Changes are saved for this session", color = Muted.copy(alpha = .65f), fontSize = 10.sp)
                    }
                    item(key = "summaries", contentType = "summaries") {
                        BoxWithConstraints {
                            val columns = if (maxWidth >= 560.dp) 4 else 2
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                (0..3).toList().chunked(columns).forEach { indices ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        indices.forEach { index ->
                                            Summary(index, state, Modifier.weight(1f)) {
                                                detail = listOf("Relationship Score", "Social Battery", "Promise Tracker", "Calendar")[index]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (desktop) {
                        item(key = "relationship", contentType = "card-row") {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                BatteryCard(state, Modifier.weight(1f)) { detail = "Social Battery" }
                                EventsCard(Modifier.weight(1f)) { detail = "Calendar" }
                                PromisesCard(state, Modifier.weight(1f), {
                                    viewModel.handleEvent(DashboardEvent.TogglePromise(it))
                                }) { detail = "Promise Tracker" }
                            }
                        }
                    } else {
                        item(key = "events", contentType = "card") {
                            EventsCard(Modifier.fillMaxWidth()) { detail = "Calendar" }
                        }
                        item(key = "relationship", contentType = "card-row") {
                            AdaptivePair(
                                { m -> BatteryCard(state, m) { detail = "Social Battery" } },
                                { m ->
                                    PromisesCard(state, m, {
                                        viewModel.handleEvent(DashboardEvent.TogglePromise(it))
                                    }) { detail = "Promise Tracker" }
                                }
                            )
                        }
                    }
                    item(key = "finance", contentType = "card-row") {
                        if (desktop) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                FinanceCard(state.financialOverview, state.financialError, Modifier.weight(1f), onOpenFinance)
                                SpendingCard(state.financialOverview, state.financialError, Modifier.weight(1.1f))
                                SavingsCard(Modifier.weight(.9f)) { detail = "Shared Goals" }
                            }
                        } else {
                            AdaptivePair(
                                { modifier -> FinanceCard(state.financialOverview, state.financialError, modifier, onOpenFinance) },
                                { modifier -> SavingsCard(modifier) { detail = "Shared Goals" } },
                            )
                        }
                    }
                    if (!desktop) item(key = "spending", contentType = "card") {
                        SpendingCard(state.financialOverview, state.financialError, Modifier.fillMaxWidth())
                    }
                    if (!wide) item(key = "activity", contentType = "card") {
                        ActivityCard(state) { detail = "Recent Activity" }
                    }
                    item(key = "footer", contentType = "spacer") { Spacer(Modifier.height(8.dp)) }
                }
                if (wide) Column(Modifier.width(260.dp).fillMaxHeight().border(1.dp, Edge).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(28.dp))
                    ActivityCard(state) {
                        detail = "Recent Activity"
                    }
                    QuickActions {
                        if (it == "Add Expense") onAddExpense() else action = it
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
                            else -> if (i == 4) onOpenFinance() else detail = listOf("", "Social Battery", "", "Promise Tracker", "Finance Manager")[i]
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
                        if (option == "Add Expense") {
                            action = null
                            onAddExpense()
                        } else {
                            action = option
                        }
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
                    Text("Write something")
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
