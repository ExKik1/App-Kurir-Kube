package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Gradient
val KubeBlueBright = Color(0xFF00B4DB)
val KubeBlueMedium = Color(0xFF0098C7)
val KubeBlueDark = Color(0xFF0083B0)
val KubeNavyDeep = Color(0xFF072B4A)

// Glass & Surface Colors
val KubeBackgroundLight = Color(0xFFF5F7FA)
val KubeCardLight = Color(0xFFE3F2FD)
val KubeDarkCard = Color(0xFF0C2B44)
val KubeDarkCardSecondary = Color(0xFF133959)

// Accent & Status Colors
val KubeCyan = Color(0xFF00E5FF)
val KubeIceLight = Color(0xFFD6F6FF)
val KubeGreen = Color(0xFF00C853)
val KubeOrange = Color(0xFFFF9800)
val KubeRed = Color(0xFFEF5350)

// Text Colors
val KubeTextDark = Color(0xFF0A2540)
val KubeTextMuted = Color(0xFF5A738E)
val KubeTextLight = Color(0xFFFFFFFF)

// Standard Kube Liquid Sky Gradient Brush
val KubeSkyGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF00B4DB),
        Color(0xFF0096C7),
        Color(0xFF0077B6)
    )
)

val KubeDarkGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0083B0),
        Color(0xFF082D4B),
        Color(0xFF051D30)
    )
)
