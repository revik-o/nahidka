package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import nahidka.shared.ui.social_battery.generated.resources.Res
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_charge_high
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_charge_low
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_charge_medium
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

internal enum class BatteryCharge(val minimumPercentage: Int, val description: StringResource) {
    LOW(0, Res.string.socialbattery_charge_low),
    MEDIUM(30, Res.string.socialbattery_charge_medium),
    HIGH(70, Res.string.socialbattery_charge_high);

    val color: Color
        @Composable
        @ReadOnlyComposable
        get() = when (this) {
            LOW -> MaterialTheme.colorScheme.error
            MEDIUM -> MaterialTheme.colorScheme.secondary
            HIGH -> MaterialTheme.colorScheme.primary
        }

    companion object {

        fun of(socialBattery: SocialBattery): BatteryCharge =
            entries.last { batteryCharge -> socialBattery.percentage >= batteryCharge.minimumPercentage }
    }
}
