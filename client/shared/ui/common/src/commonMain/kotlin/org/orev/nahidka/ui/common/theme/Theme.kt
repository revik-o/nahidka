package org.orev.nahidka.ui.common.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun NahidkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NahidkaDarkColorScheme else NahidkaLightColorScheme,
        typography = NahidkaTypography,
        content = content,
    )
}
