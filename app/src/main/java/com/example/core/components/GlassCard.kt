package com.example.core.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Surface
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Background with #F5F7FA clean base and ambient colorful liquid/ice glow orbs
 * positioned strategically so that frosted glass cards placed on top produce genuine,
 * vivid optical refraction and glassmorphism translucency.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        // Ambient colorful liquid orbs behind the glass surfaces
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top-Right Cyan Liquid Glow Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00B4DB).copy(alpha = 0.35f),
                        Color(0xFF00E5FF).copy(alpha = 0.20f),
                        Color(0xFFF5F7FA).copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.85f, h * 0.12f),
                    radius = w * 0.65f
                )
            )

            // Top-Left Ice Blue Atmosphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0083B0).copy(alpha = 0.28f),
                        Color(0xFF80DEEA).copy(alpha = 0.15f),
                        Color(0xFFF5F7FA).copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.10f, h * 0.08f),
                    radius = w * 0.55f
                )
            )

            // Center Mid-Screen Liquid Blob (under Jobdesk and Orders)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00B4DB).copy(alpha = 0.22f),
                        Color(0xFF4DD0E1).copy(alpha = 0.12f),
                        Color(0xFFF5F7FA).copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.50f, h * 0.45f),
                    radius = w * 0.70f
                )
            )

            // Lower Screen Deep Cyan Ambient Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0288D1).copy(alpha = 0.25f),
                        Color(0xFF00E5FF).copy(alpha = 0.10f),
                        Color(0xFFF5F7FA).copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.25f, h * 0.80f),
                    radius = w * 0.60f
                )
            )
        }

        // Subtly blended background watermark logo (opacity ~0.065) positioned centered in background
        Image(
            painter = painterResource(id = R.drawable.logo_kube_cube),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alpha = 0.065f,
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.Center)
        )

        // Child composables (cards, lists, headers) sit directly on top of the liquid canvas
        content()
    }
}

/**
 * Authentic Liquid Glass Card with:
 * 1. Semi-transparent white specular gradient fill (20% - 40% opacity)
 * 2. Crisp, beveled white specular border stroke (70% - 30% alpha)
 * 3. Soft diffused drop shadow for floating glass plate depth
 * 4. Inner specular light sheen
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 6.dp,
    fillAlphaTop: Float = 0.38f,
    fillAlphaBottom: Float = 0.20f,
    tintColor: Color = Color.White,
    borderAlphaTop: Float = 0.85f,
    borderAlphaBottom: Float = 0.30f,
    borderWidth: Dp = 1.2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    val glassFillBrush = Brush.linearGradient(
        colors = listOf(
            tintColor.copy(alpha = fillAlphaTop),
            tintColor.copy(alpha = fillAlphaBottom)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val glassBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = borderAlphaTop),
            Color.White.copy(alpha = borderAlphaBottom)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Surface(
        modifier = clickableModifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x220083B0),
                spotColor = Color(0x220083B0),
                clip = false
            )
            .clip(shape)
            .border(BorderStroke(borderWidth, glassBorderBrush), shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(glassFillBrush)
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * Dark Frosted Liquid Glass Card (used for Performance metric and Timeline sections)
 */
@Composable
fun LiquidDarkGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 8.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    val darkGlassFill = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0A2B47).copy(alpha = 0.85f),
            Color(0xFF061E33).copy(alpha = 0.92f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val darkGlassBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color(0xFF00E5FF).copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    Surface(
        modifier = clickableModifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x33000000),
                spotColor = Color(0x33000000),
                clip = false
            )
            .clip(shape)
            .border(BorderStroke(1.dp, darkGlassBorder), shape),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(darkGlassFill)
                .padding(18.dp),
            content = content
        )
    }
}

// Backward-compatibility aliases
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.30f),
    borderColor: Color = Color.White.copy(alpha = 0.70f),
    borderWidth: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        fillAlphaTop = 0.35f,
        fillAlphaBottom = 0.20f,
        borderWidth = borderWidth,
        onClick = onClick,
        content = content
    )
}

@Composable
fun DarkGlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0A2B47).copy(alpha = 0.88f),
    borderColor: Color = Color.White.copy(alpha = 0.20f),
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidDarkGlassCard(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        onClick = onClick,
        content = content
    )
}
