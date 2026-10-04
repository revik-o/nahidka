package org.orev.nahidka.ui.common.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

val NahidkaBackground = Color(0xFF090913)
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

val NahidkaLightColorScheme = lightColorScheme(
    primary = Color(0xFF8E2685),
    onPrimary = Color.White,
    secondary = Color(0xFF6633AE),
    onSecondary = Color.White,
    background = Color(0xFFFCF8FF),
    onBackground = Color(0xFF211A25),
    surface = Color(0xFFFCF8FF),
    onSurface = Color(0xFF211A25),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)
