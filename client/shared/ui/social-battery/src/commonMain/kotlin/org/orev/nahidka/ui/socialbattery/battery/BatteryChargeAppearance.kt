package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import nahidka.shared.ui.social_battery.generated.resources.Res
import nahidka.shared.ui.social_battery.generated.resources.socialbattery_unset
import org.jetbrains.compose.resources.StringResource
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

internal val SocialBattery?.chargeColor: Color
    @Composable
    @ReadOnlyComposable
    get() = this
        ?.let(BatteryCharge::of)
        ?.color
        ?: MaterialTheme.colorScheme.outline

internal val SocialBattery?.chargeDescription: StringResource
    get() = this
        ?.let(BatteryCharge::of)
        ?.description
        ?: Res.string.socialbattery_unset
