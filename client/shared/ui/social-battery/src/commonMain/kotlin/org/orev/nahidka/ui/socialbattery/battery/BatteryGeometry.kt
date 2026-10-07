package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

internal const val BATTERY_ASPECT_RATIO = 0.54f

private const val TERMINAL_WIDTH_FRACTION = 0.26f
private const val TERMINAL_HEIGHT_FRACTION = 0.06f
private const val TERMINAL_CORNER_RADIUS_FRACTION = 0.03f
private const val CASING_STROKE_WIDTH_FRACTION = 0.035f
private const val CASING_CORNER_RADIUS_FRACTION = 0.12f
private const val CELL_GAP_FRACTION = 0.035f

internal class BatteryGeometry(batterySize: Size) {

    val casingStrokeWidth = batterySize.width * CASING_STROKE_WIDTH_FRACTION

    val terminal = Rect(
        left = batterySize.width * (1 - TERMINAL_WIDTH_FRACTION) / 2,
        top = 0f,
        right = batterySize.width * (1 + TERMINAL_WIDTH_FRACTION) / 2,
        bottom = batterySize.height * TERMINAL_HEIGHT_FRACTION + casingStrokeWidth,
    )

    val terminalCornerRadius = CornerRadius(batterySize.width * TERMINAL_CORNER_RADIUS_FRACTION)

    val casing = Rect(
        left = casingStrokeWidth / 2,
        top = batterySize.height * TERMINAL_HEIGHT_FRACTION + casingStrokeWidth / 2,
        right = batterySize.width - casingStrokeWidth / 2,
        bottom = batterySize.height - casingStrokeWidth / 2,
    )

    val casingCornerRadius = CornerRadius(batterySize.width * CASING_CORNER_RADIUS_FRACTION)

    val cell = casing.deflate(casingStrokeWidth / 2 + batterySize.width * CELL_GAP_FRACTION)

    val cellCornerRadius = CornerRadius(casingCornerRadius.x - (casing.left - cell.left))

    fun chargeArea(chargeFraction: Float): Rect =
        cell.copy(top = cell.bottom - cell.height * chargeFraction)

    fun chargeFractionAt(verticalPosition: Float): Float =
        ((cell.bottom - verticalPosition) / cell.height).coerceIn(0f, 1f)
}
