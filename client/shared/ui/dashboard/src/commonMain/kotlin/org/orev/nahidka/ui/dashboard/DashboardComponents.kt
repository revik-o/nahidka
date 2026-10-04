package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Ink = Color(0xFF090913)
internal val Panel = Color(0xFF141322)
internal val Edge = Color(0xFF29243E)
internal val Muted = Color(0xFFB4AFCE)
internal val Purple = Color(0xFF963CFF)
internal val Pink = Color(0xFFF34DA9)
internal val Mint = Color(0xFF19E4C1)

@Composable
internal fun AdaptivePair(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    BoxWithConstraints {
        if (maxWidth >= 620.dp) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        } else Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun Tile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Panel, Color(0xFF0D0D19)))).border(1.dp, Edge, RoundedCornerShape(17.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
internal fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Color(0xFFF5F2FF), fontSize = 14.sp, fontWeight = FontWeight.Medium)
}

@Composable
internal fun Caption(text: String) {
    Text(text, color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
}

@Composable
internal fun Link(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
        Text(text, color = Color(0xFFCDB5FF), fontSize = 11.sp)
    }
}

@Composable
internal fun CardTitle(title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Label(title, Modifier.weight(1f))
        Link("See all", onClick)
    }
}

@Composable
internal fun Brand(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(38.dp).background(Brush.linearGradient(listOf(Purple, Pink)), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text("♥", color = Color.White, fontSize = 29.sp)
        }
        Column {
            Text("nahidka", fontWeight = FontWeight.Bold, fontSize = 23.sp, color = Color.White)
            Caption("for couples")
        }
    }
}

@Composable
internal fun Sidebar(onSelect: (String) -> Unit) {
    Column(Modifier.width(225.dp).fillMaxHeight().border(1.dp, Edge).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.height(25.dp))
        Brand()
        Spacer(Modifier.height(24.dp))
        listOf("⌂" to "Dashboard", "ϟ" to "Social Battery", "✓" to "Promise Tracker", "▣" to "Finance Manager", "▦" to "Calendar", "♡" to "Shared Goals", "▤" to "Notes", "▱" to "Files", "▥" to "Analytics", "⚙" to "Settings").forEachIndexed {
            i, (icon, title) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (i == 0) Purple.copy(alpha = .16f) else Color.Transparent).clickable {
                if (i != 0) onSelect(title)
            }
            .padding(vertical = 12.dp, horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = if (i == 0) Purple else Muted, fontSize = 22.sp)
                Text(title, color = if (i == 0) Color.White else Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        listOf("A" to "You", "J" to "Your Partner").forEach {
            (initial, name) -> Tile(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Avatar(initial)
                    Column {
                        Label(name)
                        Text("● Online", color = Mint, fontSize = 10.sp)
                    }
                }
            }
        }
        Tile {
            Label("💗 Connected")
            Caption("Since May 20, 2023")
        }
    }
}

@Composable
internal fun Hero(desktop: Boolean, onAction: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(if (desktop) 120.dp else 160.dp)) {
        NightLandscape(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.65f))
        Column(Modifier.align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Good evening, Alex 👋", color = Color.White, fontSize = if (desktop) 24.sp else 23.sp, fontWeight = FontWeight.SemiBold)
            Text(if (desktop) "Here’s what’s happening in your relationship today." else "Here’s what’s happening\nin your relationship today.", color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
        }
        if (desktop) Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            OutlinedButton(onClick = onAction, border = BorderStroke(1.dp, Purple), colors = ButtonDefaults.outlinedButtonColors(containerColor = Purple.copy(alpha = .2f))) {
                Text("＋ Quick Action", color = Color.White, fontSize = 11.sp)
            }
            Caption("May 24, 2024 · Friday")
        }
    }
}

@Composable
internal fun Summary(index: Int, state: DashboardState, modifier: Modifier, onClick: () -> Unit) {
    Tile(modifier.height(210.dp).clickable(onClick = onClick)) {
        Label(listOf("♡  Relationship Score", "ϟ  Social Battery", "✓  Active Promises", "▦  Anniversary")[index])
        when (index) {
            0 -> {
                Text("92%", fontSize = 32.sp, color = Color.White)
                Caption("Amazing! Keep going 💜")
                Sparkline()
            }
            1 -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Ring(state.socialBatteryLevel, "${(state.socialBatteryLevel * 100).toInt()}%", Modifier.size(78.dp))
                Caption("Both charged")
                Text("+12% from yesterday", color = Mint, fontSize = 10.sp)
            }
            2 -> {
                Text(state.promises.count {
                    !it.completed
                }
                .toString(), fontSize = 32.sp, color = Color.White)
                Caption("Active promises")
                Caption("1 due this week")
            }
            3 -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("3", fontSize = 32.sp, color = Color.White)
                    Text(" days   ♥", color = Pink, fontSize = 20.sp)
                }
                Caption("Our Anniversary")
            }
        }
    }
}

@Composable
internal fun Avatar(initial: String) {
    Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(Color(0xFF62668D), Color(0xFF34213F))), CircleShape).border(1.dp, Purple.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) {
        Text(initial, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
internal fun BatteryCard(state: DashboardState, modifier: Modifier, onClick: () -> Unit) {
    Tile(modifier.heightIn(min = 280.dp)) {
        Label("Social Battery")
        Caption("How you both feel right now")
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar("A")
                Label("${(state.socialBatteryLevel * 100).toInt()}%")
            }
            Ring(.87f, "♥", Modifier.size(112.dp), true)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar("J")
                Label("85%")
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Caption("Great balance! You’re in sync 💜")
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Link("See details", onClick)
        }
    }
}
internal val eventRows = listOf(listOf("♥", "Our Anniversary ❤️", "May 27, 2024", "3 days"), listOf("✈", "Weekend Trip", "June 1 – June 2", "8 days"), listOf("🎁", "Partner’s Birthday 🎉", "July 14, 2024", "52 days"))

@Composable
internal fun EventsCard(modifier: Modifier, onClick: () -> Unit) {
    Tile(modifier.heightIn(min = 280.dp)) {
        CardTitle("Today & Upcoming", onClick)
        eventRows.forEachIndexed {
            i, row -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val tint = listOf(Pink, Color(0xFF55BDFF), Color(0xFFFFBF3C))[i]
                Box(Modifier.size(40.dp).background(tint.copy(alpha = .15f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                    Text(row[0], color = tint, fontSize = 24.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(row[1], color = Color.White, fontSize = 12.sp)
                    Caption(row[2])
                }
                Text(row[3], color = tint, fontSize = 11.sp)
            }
        }
    }
}

@Composable
internal fun PromisesCard(state: DashboardState, modifier: Modifier, toggle: (Int) -> Unit, onClick: () -> Unit) {
    Tile(modifier.heightIn(min = 280.dp)) {
        CardTitle("Active Promises", onClick)
        state.promises.take(3).forEach {
            promise -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(promise.completed, {
                    toggle(promise.identifier)
                }
                , modifier = Modifier.size(32.dp), colors = CheckboxDefaults.colors(checkedColor = Color(0xFF51446F), uncheckedColor = Color(0xFF74698D)))
                Column(Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    Text(promise.title, color = if (promise.completed) Muted else Color.White, fontSize = 12.sp, textDecoration = if (promise.completed) TextDecoration.LineThrough else null)
                    Caption(if (promise.completed) "Completed" else promise.date)
                }
                val color = when (promise.priority) {
                    "High" -> Pink
                    "Medium" -> Color(0xFFFFBE3B)
                    else -> Mint
                }
                Text(promise.priority, Modifier.background(color.copy(alpha = .08f), CircleShape).padding(6.dp), color = color, fontSize = 10.sp)
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
internal fun FinanceCard(summary: FinancialOverviewUi?, error: String?, modifier: Modifier, onClick: () -> Unit) {
    Tile(modifier.heightIn(min = 280.dp)) {
        Label("Finance Overview")
        Caption("This month")
        if (summary == null) {
            Caption(error ?: "Loading this month’s financial summary")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(summary.formattedSpent, color = Color.White, fontSize = 23.sp)
                Caption("Net spent this month")
                Spacer(Modifier.height(9.dp))
                Caption("Posted income")
                Text(summary.formattedIncome, color = Mint, fontSize = 16.sp)
                Spacer(Modifier.height(5.dp))
                Caption(summary.formattedAvailableAfterPlanning?.let { "Available after planning · $it" }
                    ?: "Set up a monthly plan to see funds after planning")
            }
        }
        Link("Go to Finance", onClick)
    }
}

@Composable
internal fun SavingsCard(modifier: Modifier, onClick: () -> Unit) {
    Tile(modifier.heightIn(min = 280.dp)) {
        Label("Savings Goal")
        Spacer(Modifier.height(10.dp))
        Caption("New Camera")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("72%", color = Color.White, fontSize = 34.sp)
                Caption("$1,440 of $2,000")
            }
            CameraArt(Modifier.size(86.dp))
        }
        Spacer(Modifier.height(8.dp))
        Progress(.72f, Purple)
        Link("View Goals", onClick)
    }
}

@Composable
internal fun SpendingCard(summary: FinancialOverviewUi?, error: String?, modifier: Modifier) {
    Tile(modifier.heightIn(min = 280.dp)) {
        Label("Monthly Spending")
        if (summary == null) {
            Caption(error ?: "Loading this month’s category totals")
        } else {
            Caption("${summary.spending.period.month} · ${summary.assetDisplayCode}")
            CategorySpendingDonut(summary)
        }
    }
}

@Composable
internal fun ActivityCard(state: DashboardState, onClick: () -> Unit) {
    Tile(Modifier.fillMaxWidth()) {
        CardTitle("Recent Activity", onClick)
        state.activity.take(5).forEachIndexed {
            i, entry -> Row(Modifier.padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Avatar(if (i % 2 == 0) "J" else "A")
                Column {
                    Text(if (i % 2 == 0) "Your partner" else "You", color = Color.White, fontSize = 12.sp)
                    Caption(entry)
                    Text(if (i < 2) "2h ago" else "Yesterday", color = Muted.copy(alpha = .6f), fontSize = 10.sp)
                }
            }
        }
        Link("View all activity", onClick)
    }
}

@Composable
internal fun QuickActions(onAction: (String) -> Unit) {
    Tile {
        Label("Quick Actions")
        listOf("Add Promise" to "Add Expense", "New Note" to "Add Memory").forEach {
            (a, b) -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(a, b).forEach {
                    name -> Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Purple.copy(alpha = .1f)).clickable {
                        onAction(name)
                    }
                    .padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (name == "Add Memory") "♥" else "+", color = Pink, fontSize = 24.sp)
                        Text(name, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
