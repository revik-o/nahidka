package org.orev.nahidka.ui.common.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme

val NahidkaBackground = Color(0xFF0A0A14)
val NahidkaPrimary = Color(0xFFD042C3)
val NahidkaSecondary = Color(0xFF7B42D0)
val NahidkaSurface = Color(0xFF151523)
val NahidkaOnBackground = Color(0xFFE0E0E0)
val NahidkaOnPrimary = Color(0xFFFFFFFF)
val NahidkaOnSecondary = Color(0xFFFFFFFF)
val NahidkaOnSurface = Color(0xFFE0E0E0)
val NahidkaError = Color(0xFFCF6679)
val NahidkaOnError = Color(0xFF000000)

val NahidkaDarkColorScheme = darkColorScheme(
    primary = NahidkaPrimary,
    onPrimary = NahidkaOnPrimary,
    secondary = NahidkaSecondary,
    onSecondary = NahidkaOnSecondary,
    background = NahidkaBackground,
    onBackground = NahidkaOnBackground,
    surface = NahidkaSurface,
    onSurface = NahidkaOnSurface,
    error = NahidkaError,
    onError = NahidkaOnError
)
