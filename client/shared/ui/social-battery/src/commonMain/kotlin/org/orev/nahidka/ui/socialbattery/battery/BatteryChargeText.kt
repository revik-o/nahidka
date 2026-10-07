package org.orev.nahidka.ui.socialbattery.battery

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import org.orev.nahidka.feature.socialbattery.dto.SocialBattery

@Composable
internal fun BatteryChargeText(
    socialBattery: SocialBattery?,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(socialBattery.chargeDescription),
        modifier = modifier,
        color = socialBattery.chargeColor,
        textAlign = textAlign,
        style = MaterialTheme.typography.titleMedium,
    )
}
