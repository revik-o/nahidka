package org.orev.nahidka.ui.financialmanagement.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.financial_management.generated.resources.Res
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_spending_empty
import nahidka.shared.ui.financial_management.generated.resources.financialmanagement_spending_net
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.ui.common.component.SupportingText

private const val DONUT_START_ANGLE = -90f
private const val FULL_TURN_DEGREES = 360f
private const val DONUT_STROKE_FRACTION = 0.18f
private val DONUT_SIZE = 120.dp
private val LEGEND_MARKER_SIZE = 8.dp

private val SPENDING_SLICE_COLORS = listOf(
    Color(0xFF963CFF),
    Color(0xFFF34DA9),
    Color(0xFF19C9A9),
    Color(0xFFFFB020),
    Color(0xFF3FA9F5),
    Color(0xFF9B8CFF),
    Color(0xFFFF8066),
    Color(0xFF8A879D),
)

@Composable
internal fun FinancialSpendingDonut(financialOverview: FinancialOverview) {
    if (financialOverview.spendingSlices.isEmpty()) {
        SupportingText(stringResource(Res.string.financialmanagement_spending_empty))
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(DONUT_SIZE)
                .semantics {
                    contentDescription = financialOverview.spendingSlices.joinToString(". ") { spendingSlice ->
                        "${spendingSlice.label}: ${spendingSlice.formattedAmount}, ${spendingSlice.formattedPercentage}"
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                var sliceStartAngle = DONUT_START_ANGLE

                financialOverview.spendingSlices.forEachIndexed { sliceIndex, spendingSlice ->
                    val sliceSweepAngle = spendingSlice.sweepFraction * FULL_TURN_DEGREES

                    drawArc(
                        color = spendingSliceColor(sliceIndex),
                        startAngle = sliceStartAngle,
                        sweepAngle = sliceSweepAngle,
                        useCenter = false,
                        style = Stroke(width = size.minDimension * DONUT_STROKE_FRACTION),
                    )
                    sliceStartAngle += sliceSweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(financialOverview.formattedNetExpense, style = MaterialTheme.typography.labelLarge)
                SupportingText(
                    text = stringResource(Res.string.financialmanagement_spending_net),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            financialOverview.spendingSlices.forEachIndexed { sliceIndex, spendingSlice ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(LEGEND_MARKER_SIZE)) {
                        drawCircle(spendingSliceColor(sliceIndex))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = spendingSlice.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        SupportingText(
                            text = spendingSlice.formattedAmount,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
                    SupportingText(
                        text = spendingSlice.formattedPercentage,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun spendingSliceColor(sliceIndex: Int): Color =
    SPENDING_SLICE_COLORS[sliceIndex % SPENDING_SLICE_COLORS.size]
