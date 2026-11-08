package org.orev.nahidka.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import nahidka.shared.ui.dashboard.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PartnerAvatars(partners: List<PartnerPresence>, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        partners.forEachIndexed { index, partner ->
            val description = stringResource(
                if (partner.isOnline) Res.string.dashboard_partner_online else Res.string.dashboard_partner_offline,
                partner.name,
            )
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                tooltip = { PlainTooltip { Text(description) } },
                state = rememberTooltipState(),
            ) {
                Box(
                    Modifier.size(44.dp).semantics(mergeDescendants = true) {
                        contentDescription = description
                    },
                ) {
                    Box(
                        Modifier.size(40.dp).align(Alignment.Center)
                            .background(if (index % 2 == 0) Color(0xFFB71C1C) else Color(0xFFE53935), CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(partner.initials, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                    if (partner.isOnline) {
                        Box(
                            Modifier.align(Alignment.BottomStart).size(10.dp)
                                .background(Color(0xFF00C853), CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        )
                    }
                }
            }
        }
    }
}
