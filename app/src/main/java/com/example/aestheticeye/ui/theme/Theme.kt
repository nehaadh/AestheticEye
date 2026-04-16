package com.example.aestheticeye.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────────
// Warm, editorial, luxury-minimal — inspired by high-end interior design magazines

val AeGold   = Color(0xFFB08D57)   // warm gold accent
val AeGreen  = Color(0xFF4A7C59)   // muted sage green
val AeAmber  = Color(0xFFB87333)   // copper/amber for warnings
val AeRed    = Color(0xFF9B4040)   // dusty red for errors
val AeCream  = Color(0xFFF8F4EF)   // warm off-white background
val AeWarm   = Color(0xFFF2EDE6)   // slightly darker warm surface
val AeInk    = Color(0xFF1C1A18)   // near-black text
val AeMuted  = Color(0xFF6B6560)   // muted warm gray

private val LightColorScheme = lightColorScheme(
    primary              = AeGold,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFF5EDD8),
    onPrimaryContainer   = Color(0xFF4A3410),

    secondary            = AeMuted,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFEDE8E0),
    onSecondaryContainer = AeInk,

    tertiary             = Color(0xFF6B7E6B),
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFDCEADC),
    onTertiaryContainer  = Color(0xFF1A2E1A),

    background           = AeCream,
    onBackground         = AeInk,
    surface              = Color.White,
    onSurface            = AeInk,
    surfaceVariant       = AeWarm,
    onSurfaceVariant     = AeMuted,

    outline              = Color(0xFFCBC5BC),
    outlineVariant       = Color(0xFFE5E0D8),

    error                = AeRed,
    onError              = Color.White,
    errorContainer       = Color(0xFFF5DADA),
    onErrorContainer     = Color(0xFF6B1A1A),
)

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFFD4AC72),
    onPrimary            = Color(0xFF3A2600),
    primaryContainer     = Color(0xFF543C00),
    onPrimaryContainer   = Color(0xFFF5DFA8),

    secondary            = Color(0xFFBBB5AD),
    onSecondary          = Color(0xFF2E2A26),
    secondaryContainer   = Color(0xFF46413C),
    onSecondaryContainer = Color(0xFFD8D2CA),

    tertiary             = Color(0xFFA0B8A0),
    onTertiary           = Color(0xFF1A2E1A),
    tertiaryContainer    = Color(0xFF304830),
    onTertiaryContainer  = Color(0xFFBDD4BD),

    background           = Color(0xFF16140F),
    onBackground         = Color(0xFFECE6DC),
    surface              = Color(0xFF201E18),
    onSurface            = Color(0xFFECE6DC),
    surfaceVariant       = Color(0xFF2C2920),
    onSurfaceVariant     = Color(0xFFBBB5AD),

    outline              = Color(0xFF5C5850),
    outlineVariant       = Color(0xFF3A3730),

    error                = Color(0xFFCF8080),
    onError              = Color(0xFF4A1010),
    errorContainer       = Color(0xFF6B2525),
    onErrorContainer     = Color(0xFFF5C8C8),
)

@Composable
fun AestheticEyeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AeTypography,
        content     = content
    )
}