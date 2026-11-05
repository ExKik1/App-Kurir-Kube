package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KubeDarkColorScheme = darkColorScheme(
    primary = KubeBlueBright,
    secondary = KubeCyan,
    tertiary = KubeIceLight,
    background = KubeNavyDeep,
    surface = KubeDarkCard,
    onPrimary = Color.White,
    onSecondary = KubeNavyDeep,
    onBackground = Color.White,
    onSurface = Color.White
)

private val KubeLightColorScheme = lightColorScheme(
    primary = KubeBlueBright,
    secondary = KubeBlueDark,
    tertiary = KubeCyan,
    background = KubeBackgroundLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = KubeTextDark,
    onSurface = KubeTextDark
)

@Composable
fun KubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KubeDarkColorScheme else KubeLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    KubeTheme(darkTheme = darkTheme, content = content)
}
