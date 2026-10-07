package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.toSize
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

internal fun Modifier.batteryControl(onBatteryChange: (SocialBattery) -> Unit): Modifier =
    this
        .semantics {
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
        }

private fun PointerInputScope.socialBatteryAt(pointerPosition: Offset): SocialBattery =
    socialBatteryOf(
        BatteryGeometry(size.toSize()).chargeFractionAt(pointerPosition.y),
    )
