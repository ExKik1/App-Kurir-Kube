package com.example.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 1. BACKGROUND ATMOSPHERIC MESH (Glacial Ice -> Oceanic Teal -> Deep Navy Focal Orbs)
 * Memberikan latar gradasi mesh yang kaya warna sejuk di seluruh layar sehingga
 * kartu kaca bening (clear glassmorphism) membiaskan warna teal, aqua, dan navy
 * secara lembut tanpa area tengah putih polos.
 */
@Composable
fun KubeAtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE6F4F7), // Glacial Ice Top-Left
            Color(0xFFCDEAF0), // Cool Aqua Upper-Mid
            Color(0xFFAADBE5), // Soft Oceanic Teal Center
            Color(0xFF83C5D4)  // Deep Cool Lagoon Bottom-Right
        ),
        start = Offset(0f, 0f),
        end = Offset(1080f, 2200f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = baseGradient)
            .drawBehind {
                val w = size.width
                val h = size.height

                // Orb 1 (Kiri Atas): Soft Glacial Mint-Aqua Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB2F0E8).copy(alpha = 0.85f),
                            Color(0xFFCCF5EF).copy(alpha = 0.42f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.14f, h * 0.10f),
                        radius = w * 0.68f
                    ),
                    radius = w * 0.68f,
                    center = Offset(w * 0.14f, h * 0.10f)
                )

                // Orb 2 (Kanan Atas): Deep Oceanic Cyan-Navy Focal Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF114063).copy(alpha = 0.90f),
                            Color(0xFF1B6B8A).copy(alpha = 0.56f),
                            Color(0xFF45B3C9).copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.86f, h * 0.24f),
                        radius = w * 0.68f
                    ),
                    radius = w * 0.68f,
                    center = Offset(w * 0.86f, h * 0.24f)
                )

                // Orb 3 (Tengah): Luminous Seafoam-Teal Wash (menghilangkan tengah putih polos)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF48BFC9).copy(alpha = 0.58f),
                            Color(0xFF7AD9D1).copy(alpha = 0.34f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.48f, h * 0.48f),
                        radius = w * 0.78f
                    ),
                    radius = w * 0.78f,
                    center = Offset(w * 0.48f, h * 0.48f)
                )

                // Orb 4 (Kiri Bawah): Deep Navy-Teal Focal Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0E3454).copy(alpha = 0.94f),
                            Color(0xFF19597A).copy(alpha = 0.58f),
                            Color(0xFF399AB3).copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.10f, h * 0.80f),
                        radius = w * 0.76f
                    ),
                    radius = w * 0.76f,
                    center = Offset(w * 0.10f, h * 0.80f)
                )

                // Orb 5 (Kanan Bawah): Cool Aqua-Cyan Horizon Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF56CFE1).copy(alpha = 0.65f),
                            Color(0xFF98E8DF).copy(alpha = 0.36f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.84f, h * 0.88f),
                        radius = w * 0.74f
                    ),
                    radius = w * 0.74f,
                    center = Offset(w * 0.84f, h * 0.88f)
                )
            },
        content = content
    )
}

/**
 * 2. KARTU GLASSMORPHISM BENING UNIVERSAL (Clear Frosted & Cyan-Glow)
 * - Tanpa Modifier.shadow() agar efek kaca jernih menyatu mulus di atas mesh.
 * - Opasitas kaca rendah (0.48f -> 0.18f standar, 0.34f -> 0.12f highlighted)
 *   dengan specular top highlight dan rim border 1.2.dp.
 */
@Composable
fun KubeGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isHighlighted: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundBrush = if (isHighlighted) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.34f),
                Color(0xFFB8F1FA).copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.12f)
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 700f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.48f),
                Color(0xFFEAF8FB).copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.18f)
            ),
            start = Offset(0f, 0f),
            end = Offset(700f, 700f)
        )
    }

    val specularRimBrush = if (isHighlighted) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFA5F3FC).copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.22f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.52f),
                Color.White.copy(alpha = 0.24f)
            )
        )
    }

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = backgroundBrush)
            .drawBehind {
                // Specular top highlight lembut
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isHighlighted) Color(0xFF67E8F9).copy(alpha = 0.34f)
                            else Color.White.copy(alpha = 0.48f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.5f, 0f)
                )
            }
            .border(width = 1.2.dp, brush = specularRimBrush, shape = shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * 3. KOTAK IKON SQUIRCLE KACA
 */
@Composable
fun KubeGlassIconBox(
    icon: ImageVector,
    contentDescription: String? = null,
    tint: Color = Color(0xFF1E3A4C),
    isHighlighted: Boolean = false,
    boxSize: Dp = 46.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(15.dp)
    val bgBrush = if (isHighlighted) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.62f),
                Color(0xFFCCF4FA).copy(alpha = 0.36f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.72f),
                Color(0xFFE8F6FA).copy(alpha = 0.42f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(boxSize)
            .clip(shape)
            .background(brush = bgBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.85f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isHighlighted) Color(0xFF0284A8) else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * 4. BADGE PIL KACA
 */
@Composable
fun KubeGlassPill(
    text: String,
    textColor: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.68f),
                        Color.White.copy(alpha = 0.38f)
                    )
                )
            )
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.85f), shape = CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 5. WRAPPER KOMPATIBILITAS: LightFrostedGlassCard
 * Tetap dipertahankan agar tidak ada error kompilasi di layar lama mana pun.
 */
@Composable
fun LightFrostedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tintAlphaTop: Float = 0.48f,
    tintAlphaBottom: Float = 0.18f,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    KubeGlassCard(
        modifier = modifier,
        shape = shape,
        isHighlighted = false,
        contentPadding = PaddingValues(18.dp),
        onClick = onClick,
        content = content
    )
}

/**
 * 6. KARTU KACA GELAP (Dark Obsidian Glass)
 * Digunakan untuk kartu utama Performa Pengiriman, Timeline Peta, & Ringkasan Dompet.
 */
@Composable
fun DarkObsidianGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val rimBorder = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.42f),
            Color(0xFF38BDF8).copy(alpha = 0.24f),
            Color.White.copy(alpha = 0.12f)
        ),
        start = Offset(0f, 0f),
        end = Offset(800f, 900f)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = Color(0xFF06131A).copy(alpha = 0.45f),
                spotColor = Color(0xFF06131A).copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF132D3B).copy(alpha = 0.88f),
                        Color(0xFF0C1E28).copy(alpha = 0.94f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(700f, 900f)
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2DD4BF).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.26f, size.height * 0.46f),
                        radius = size.width * 0.42f
                    ),
                    radius = size.width * 0.42f,
                    center = Offset(size.width * 0.26f, size.height * 0.46f)
                )
            }
            .border(width = 1.2.dp, brush = rimBorder, shape = shape),
        content = content
    )
}
