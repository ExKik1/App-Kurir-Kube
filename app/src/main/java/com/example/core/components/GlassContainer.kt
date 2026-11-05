package com.example.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GlassContainer: Frosted liquid glass container with:
 * - Semi-transparent background
 * - Fine specular white border stroke
 * - Soft diffused ambient shadow
 * - Allows content underneath to remain visible with subtle glassmorphism effect
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(36.dp),
    backgroundColor: Color = Color(0xFF072B4A).copy(alpha = 0.52f),
    borderColor: Color = Color.White.copy(alpha = 0.45f),
    borderWidth: Dp = 1.2.dp,
    elevation: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassSpecularBorder = Brush.linearGradient(
        colors = listOf(
            borderColor,
            Color.White.copy(alpha = 0.15f),
            Color(0xFF00E5FF).copy(alpha = 0.25f),
            borderColor.copy(alpha = 0.30f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Surface(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x330083B0),
                spotColor = Color(0x400083B0),
                clip = false
            )
            .clip(shape)
            .border(BorderStroke(borderWidth, glassSpecularBorder), shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = (backgroundColor.alpha * 1.15f).coerceAtMost(0.95f))
                        )
                    )
                ),
            content = content
        )
    }
}
