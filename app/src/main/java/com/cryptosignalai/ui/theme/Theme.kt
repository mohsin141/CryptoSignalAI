package com.cryptosignalai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnDark,
    background = BgDark,
    onBackground = OnDark,
    surface = SurfaceDark,
    onSurface = OnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedText
)

@Composable
fun CryptoSignalTheme(
    darkTheme: Boolean = true, // app is dark-theme only by design
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
