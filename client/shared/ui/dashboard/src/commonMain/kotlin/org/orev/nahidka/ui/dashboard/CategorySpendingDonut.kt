package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val spendingColors = listOf(
    Color(0xFF963CFF),
    Color(0xFFF34DA9),
    Color(0xFF19E4C1),
    Color(0xFFFFBE3B),
    Color(0xFF55BDFF),
    Color(0xFF9B8CFF),
    Color(0xFFFF8066),
    Color(0xFF8A879D),
)

@Composable
internal fun CategorySpendingDonut(summary: FinancialOverviewUi, modifier: Modifier = Modifier) {
    if (summary.spendingSlices.isEmpty()) {
        Text("No positive net category spending in this month", color = Muted)
    } else {
        Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                Modifier.size(126.dp).semantics {
                    contentDescription = summary.spendingSlices.joinToString(". ") {
                        "${it.label}: ${it.formattedAmount}, ${formatPercentage(it.percentageBasisPoints)}"
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(126.dp)) {
                    var startAngle = -90f
                    summary.spendingSlices.forEachIndexed { index, slice ->
                        val sweep = slice.percentageBasisPoints / 10_000f * 360f
                        drawArc(
                            color = spendingColors[index % spendingColors.size],
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = size.minDimension * .18f, cap = StrokeCap.Butt),
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(summary.formattedSpent, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    Text("net", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                summary.spendingSlices.forEachIndexed { index, slice ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        Box(Modifier.size(8.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(8.dp)) { drawCircle(spendingColors[index % spendingColors.size]) }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(slice.label, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                            Text(slice.formattedAmount, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                        Text(formatPercentage(slice.percentageBasisPoints), color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    if (summary.spending.refundCredits.units > 0) {
        Spacer(Modifier.height(6.dp))
        Text("Refund credits by category: ${summary.formattedRefundCredits}", color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatPercentage(basisPoints: Int): String =
    "${basisPoints / 100}.${(basisPoints % 100).toString().padStart(2, '0')}%"
