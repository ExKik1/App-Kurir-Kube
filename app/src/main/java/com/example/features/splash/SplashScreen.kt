package com.example.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeSkyGradient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val progress = remember { Animatable(0.05f) }
    val logoScale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        progress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
        )
        delay(200)
        // Transitions cleanly with pushReplacement semantics
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KubeSkyGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(onClick = onSplashFinished)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            // Breathing/Pulsing Animation with Blue Glowing Halo Effect
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse_transition")
            val pulsingScale by infiniteTransition.animateFloat(
                initialValue = 0.90f,
                targetValue = 1.10f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulsing_scale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.75f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Glowing blue halo shadow behind ice cube
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(pulsingScale * 1.15f)
                        .clip(CircleShape)
                        .background(Color(0xFF00B4DB).copy(alpha = glowAlpha))
                        .shadow(40.dp, CircleShape, ambientColor = Color(0xFF00B4DB), spotColor = Color(0xFF00D2FF))
                )

                // Secondary soft cyan glow
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulsingScale * 1.05f)
                        .clip(CircleShape)
                        .background(Color(0xFF4AC2E8).copy(alpha = glowAlpha * 0.8f))
                )

                // Official New Ice Cube Logo with Breathing / Pulsing Scale
                Image(
                    painter = painterResource(id = R.drawable.logo_kube_cube),
                    contentDescription = "Logo Kubus Es Kube Baru",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(160.dp)
                        .scale(logoScale.value * pulsingScale)
                        .testTag("splash_cube_logo")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Es batu untuk setiap kebutuhan.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.95f)
            )

            Spacer(modifier = Modifier.height(50.dp))

            // Unified Linear Progress Indicator (Clean, non-stacked animation)
            LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = KubeBlueBright,
                trackColor = Color.White.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Loading...",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        // Bottom connection status badge
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Connecting to services",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.70f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(KubeGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Secure connection",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F2F4A)
                )
            }
        }
    }
}
