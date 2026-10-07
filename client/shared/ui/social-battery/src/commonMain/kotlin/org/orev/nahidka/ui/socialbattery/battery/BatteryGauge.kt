package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.toSize
import nahidka.shared.ui.social_battery.generated.resources.Res
import nahidka.shared.ui.social_battery.generated.resources.feature_socialbattery_title
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

private const val UNSET_PERCENTAGE_LABEL = "—"
private const val PERCENTAGE_FONT_SIZE_FRACTION = 0.2f
private const val CHARGE_TOP_ALPHA = 0.3f
private const val CHARGE_BOTTOM_ALPHA = 0.75f

@Composable
internal fun BatteryGauge(
    socialBattery: SocialBattery?,
    chargeFraction: Float,
    chargeColor: Color,
    onBatteryChange: (SocialBattery) -> Unit,
    modifier: Modifier = Modifier,
) {
    val batteryTitle = stringResource(Res.string.feature_socialbattery_title)
    val percentageLabel = socialBattery?.let { chargedBattery -> "${chargedBattery.percentage}%" } ?: UNSET_PERCENTAGE_LABEL
    val textMeasurer = rememberTextMeasurer()
    val percentageTextStyle = MaterialTheme.typography.displayMedium
    val casingColor = MaterialTheme.colorScheme.onSurface
    val cellColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .semantics {
                contentDescription = batteryTitle
                stateDescription = percentageLabel
                progressBarRangeInfo = ProgressBarRangeInfo(socialBattery.chargeFraction(), 0f..1f, CHARGE_FRACTION_STEPS)
                setProgress { targetChargeFraction ->
                    onBatteryChange(socialBatteryOf(targetChargeFraction))
                    true
                }
            }
            .pointerInput(onBatteryChange) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown()
                    onBatteryChange(socialBatteryAt(firstDown.position))
                    verticalDrag(firstDown.id) { pointerChange ->
                        pointerChange.consume()
                        onBatteryChange(socialBatteryAt(pointerChange.position))
                    }
                }
            },
    ) {
        val batteryGeometry = BatteryGeometry(size)
        val chargeArea = batteryGeometry.chargeArea(chargeFraction)
        val percentageTextLayout = textMeasurer.measure(
            text = percentageLabel,
            style = percentageTextStyle.copy(
                color = casingColor,
                fontSize = (size.width * PERCENTAGE_FONT_SIZE_FRACTION).toSp(),
            ),
        )

        drawRoundRect(
            color = casingColor,
            topLeft = batteryGeometry.terminal.topLeft,
            size = batteryGeometry.terminal.size,
            cornerRadius = batteryGeometry.terminalCornerRadius,
        )
        drawRoundRect(
            color = cellColor,
            topLeft = batteryGeometry.casing.topLeft,
            size = batteryGeometry.casing.size,
            cornerRadius = batteryGeometry.casingCornerRadius,
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(chargeColor.copy(alpha = CHARGE_TOP_ALPHA), chargeColor.copy(alpha = CHARGE_BOTTOM_ALPHA)),
                startY = chargeArea.top,
                endY = chargeArea.bottom,
            ),
            topLeft = chargeArea.topLeft,
            size = chargeArea.size,
            cornerRadius = batteryGeometry.cellCornerRadius,
        )
        drawRoundRect(
            color = casingColor,
            topLeft = batteryGeometry.casing.topLeft,
            size = batteryGeometry.casing.size,
            cornerRadius = batteryGeometry.casingCornerRadius,
            style = Stroke(batteryGeometry.casingStrokeWidth),
        )
        drawText(
            textLayoutResult = percentageTextLayout,
            topLeft = batteryGeometry.cell.center - percentageTextLayout.size.toSize().center,
        )
    }
}

private fun PointerInputScope.socialBatteryAt(pointerPosition: Offset): SocialBattery =
    socialBatteryOf(
        BatteryGeometry(size.toSize()).chargeFractionAt(pointerPosition.y),
    )
