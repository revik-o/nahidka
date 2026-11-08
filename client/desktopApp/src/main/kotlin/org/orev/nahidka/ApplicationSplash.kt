package org.orev.nahidka

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.orev.nahidka.ui.common.component.BrandMark
import org.orev.nahidka.ui.common.theme.NahidkaBackground
import org.orev.nahidka.ui.common.theme.NahidkaOnBackground

private val SPLASH_BRAND_MARK_SIZE = 96.dp

@Composable
fun ApplicationSplash(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NahidkaBackground),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(Modifier.size(SPLASH_BRAND_MARK_SIZE))
        Text(
            text = "Nahidka",
            color = NahidkaOnBackground,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}