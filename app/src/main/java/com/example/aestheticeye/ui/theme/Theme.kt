package com.example.aestheticeye.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Indigo600 = Color(0xFF4F46E5)
val Indigo50  = Color(0xFFEEF2FF)
val Gray50    = Color(0xFFF9FAFB)
val Gray100   = Color(0xFFF3F4F6)
val Gray200   = Color(0xFFE5E7EB)
val Gray800   = Color(0xFF1F2937)
val White     = Color(0xFFFFFFFF)
val Green500  = Color(0xFF22C55E)
val Yellow500 = Color(0xFFEAB308)
val Red500    = Color(0xFFEF4444)

private val LightColors = lightColorScheme(
    primary          = Indigo600,
    onPrimary        = White,
    primaryContainer = Indigo50,
    secondary        = Gray800,
    onSecondary      = White,
    background       = Gray100,
    onBackground     = Gray800,
    surface          = White,
    onSurface        = Gray800,
    surfaceVariant   = Gray50,
    outline          = Gray200,
    error            = Red500,
    onError          = White
)

@Composable
fun AestheticEyeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}