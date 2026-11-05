package com.example.features.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark

@Composable
fun VectorMapCanvas(
    distanceText: String = "2,3 km",
    etaText: String = "± 8 menit",
    onMapClick: () -> Unit = {}
) {
    // Subtle pulsating animation for the courier location
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "truck_pos"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFE8F5E9))
            .clickable(onClick = onMapClick)
    ) {
        // Custom Vector Street Map Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background terrain shapes (parks, river)
            drawCircle(
                color = Color(0xFFDFF0E8),
                radius = w * 0.4f,
                center = Offset(w * 0.9f, h * 0.3f)
            )

            // River water body
            val riverPath = Path().apply {
                moveTo(w * 0.35f, 0f)
                cubicTo(w * 0.40f, h * 0.4f, w * 0.25f, h * 0.7f, w * 0.30f, h)
                lineTo(w * 0.42f, h)
                cubicTo(w * 0.38f, h * 0.7f, w * 0.50f, h * 0.4f, w * 0.46f, 0f)
                close()
            }
            drawPath(path = riverPath, color = Color(0xFFCFEAF7))

            // White Street Grid lines
            val roadColor = Color.White
            val roadStroke = 18f

            // Diagonal road 1
            drawLine(
                color = roadColor,
                start = Offset(-20f, h * 0.6f),
                end = Offset(w + 20f, h * 0.1f),
                strokeWidth = roadStroke,
                cap = StrokeCap.Round
            )
            // Diagonal road 2
            drawLine(
                color = roadColor,
                start = Offset(-20f, h * 0.2f),
                end = Offset(w + 20f, h * 0.85f),
                strokeWidth = roadStroke,
                cap = StrokeCap.Round
            )
            // Cross road
            drawLine(
                color = roadColor,
                start = Offset(w * 0.2f, -20f),
                end = Offset(w * 0.8f, h + 20f),
                strokeWidth = 24f,
                cap = StrokeCap.Round
            )
            // Secondary cross road
            drawLine(
                color = roadColor,
                start = Offset(w * 0.7f, -20f),
                end = Offset(w * 0.3f, h + 20f),
                strokeWidth = 16f,
                cap = StrokeCap.Round
            )

            // Dynamic Active Route Path (Cyan-Blue line with glow)
            val routePath = Path().apply {
                moveTo(w * 0.15f, h * 0.65f)
                lineTo(w * 0.30f, h * 0.45f)
                lineTo(w * 0.48f, h * 0.52f)
                cubicTo(w * 0.65f, h * 0.52f, w * 0.68f, h * 0.40f, w * 0.68f, h * 0.32f)
                lineTo(w * 0.68f, h * 0.22f)
            }

            // Route outer glow
            drawPath(
                path = routePath,
                color = Color(0xFF00B4DB).copy(alpha = 0.3f),
                style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Route primary line
            drawPath(
                path = routePath,
                color = Color(0xFF0288D1),
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Start Depot Point (Depot circle)
            drawCircle(
                color = Color.White,
                radius = 16f,
                center = Offset(w * 0.15f, h * 0.65f)
            )
            drawCircle(
                color = Color(0xFF0288D1),
                radius = 11f,
                center = Offset(w * 0.15f, h * 0.65f)
            )

            // End Destination Pin Circle
            drawCircle(
                color = Color(0xFFE53935),
                radius = 10f,
                center = Offset(w * 0.68f, h * 0.22f)
            )
        }

        // Animated Courier Truck Icon along the road
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(38.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(KubeBlueBright)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.LocalShipping,
                contentDescription = "Kurir Kube Sedang Bergerak",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // End Destination Pin Icon
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 120.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Tujuan",
                tint = Color(0xFFE53935),
                modifier = Modifier.size(32.dp)
            )
        }

        // Floating Distance Badge on Top-Right
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = KubeBlueBright,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = distanceText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = etaText,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
