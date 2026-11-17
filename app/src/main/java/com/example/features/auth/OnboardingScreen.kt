package com.example.features.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.KubeSkyGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    onContinue: () -> Unit
) {
    val bottomSheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KubeSkyGradient)
            .testTag("onboarding_screen")
    ) {
        // Subtle ambient light orbs behind both the top logo and bottom Liquid Glass card
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(36.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF80DEEA).copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.width * 0.55f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.72f),
                    radius = size.width * 0.50f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                    radius = size.width * 0.48f
                )
            )
        }

        // Single-screen vertical layout: top-center logo + bottom flush Liquid Glass card
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top-Center Area: Official Kube Ice Cube Logo with Soft Ambient Glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer soft diffused halo glow
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.42f),
                                        Color(0xFF00E5FF).copy(alpha = 0.24f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .blur(26.dp)
                    )
                    // Inner crisp aura ring
                    Box(
                        modifier = Modifier
                            .size(152.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    )
                    Image(
                        painter = painterResource(id = R.drawable.logo_kube_cube),
                        contentDescription = "Logo Kubus Kube",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(176.dp)
                    )
                }
            }

            // Bottom Liquid Glass Card: curved top corners (32.dp) ONLY, flush to the bottom edge (zero blue gap)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(bottomSheetShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.30f),
                                Color.White.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.72f),
                                Color.White.copy(alpha = 0.22f)
                            )
                        ),
                        shape = bottomSheetShape
                    )
            ) {
                // Internal frosted glass light sheen layer (blurred background only, text remains sharp)
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(28.dp)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.24f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.20f, size.height * 0.12f),
                            radius = size.width * 0.55f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF80DEEA).copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.80f),
                            radius = size.width * 0.50f
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 34.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fast, Safe, and\nAlways On Time",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Percayakan pada kurir Steadfast Kube untuk pengantaran es batu balok & kristal yang cepat, aman, dan tepat waktu.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.94f),
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    InteractiveSlideToContinueBar(
                        onSlideComplete = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Interactive Slide To Continue Bar:
 * - Track menggunakan gaya Liquid Glass transparan dengan border putih tipis
 * - Kapsul gelap 'Continue' berada di posisi awal sebelah kiri (offsetX = 0f)
 * - Teks petunjuk 'Slide to Continue →' berada di sebelah kanan kapsul
 * - Digeser dari kiri ke kanan untuk navigasi ke halaman Login
 */
@Composable
private fun InteractiveSlideToContinueBar(
    onSlideComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var innerWidthPx by remember { mutableFloatStateOf(0f) }
    var hasTriggered by remember { mutableStateOf(false) }
    val thumbWidthDp = 118.dp
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { thumbWidthDp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val currentOnSlideComplete by rememberUpdatedState(onSlideComplete)

    val maxDragPx = (innerWidthPx - thumbWidthPx).coerceAtLeast(0f)
    val progress = if (maxDragPx > 0f) (offsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(31.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .border(
                width = 1.2.dp,
                color = Color.White.copy(alpha = 0.60f),
                shape = RoundedCornerShape(31.dp)
            )
            .padding(4.dp)
            .onSizeChanged { innerWidthPx = it.width.toFloat() }
            .testTag("slide_to_continue_track")
    ) {
        // Subtle glowing trail behind the sliding capsule
        if (progress > 0.02f) {
            val fillWidthDp = with(density) { (offsetX.value + thumbWidthPx).toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(fillWidthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color(0xFF00E5FF).copy(alpha = 0.22f * progress))
            )
        }

        // Right-aligned hint label: 'Slide to Continue →' to the right of the left capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(start = thumbWidthDp + 8.dp, end = 16.dp)
                .alpha((1f - progress * 1.6f).coerceIn(0f, 1f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Slide to Continue",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f),
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Geser ke kanan",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Draggable Dark Capsule Thumb 'Continue' starting on the FAR LEFT
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .width(thumbWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(27.dp))
                .background(Color(0xFF0A2540))
                .border(
                    width = 1.dp,
                    color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                    shape = RoundedCornerShape(27.dp)
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (!hasTriggered) {
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + delta).coerceIn(0f, maxDragPx)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    },
                    onDragStopped = {
                        if (hasTriggered) return@draggable
                        if (maxDragPx > 0f && offsetX.value >= maxDragPx * 0.65f) {
                            hasTriggered = true
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    maxDragPx,
                                    animationSpec = tween(140)
                                )
                                currentOnSlideComplete()
                                delay(350)
                                offsetX.snapTo(0f)
                                hasTriggered = false
                            }
                        } else {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    }
                )
                .testTag("onboarding_continue_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Continue",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
