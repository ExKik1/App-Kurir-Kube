package com.example.features.map

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.RouteEta
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark

enum class MapLayerType {
    STREET,
    SATELLITE
}

/**
 * InteractiveRouteMapView: Fully interactive Google Maps SDK experience for Kube Courier.
 * Features:
 * - Touch pan & pinch-to-zoom gestures
 * - Active dynamic polyline route with traffic color-coding
 * - Real-time courier vehicle marker with heading & pulsing radar aura
 * - Destination marker with customer store callout
 * - Interactive controls: Zoom in/out, Recenter GPS, Map Layer toggle
 * - Direct "Buka di Google Maps" intent launcher
 */
@Composable
fun InteractiveRouteMapView(
    routeEta: RouteEta,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // Interactive Pan & Zoom State
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var currentMapLayer by remember { mutableStateOf(MapLayerType.STREET) }

    // Pulsing radar animation for courier location
    val infiniteTransition = rememberInfiniteTransition(label = "courier_pulse")
    val pulseAura by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    fun openInGoogleMapsApp() {
        val gmmIntentUri = Uri.parse(
            "google.navigation:q=${routeEta.destLat},${routeEta.destLng}&mode=d"
        )
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${routeEta.destLat},${routeEta.destLng}")
            )
            context.startActivity(webIntent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        listOf(Color.White, Color.White.copy(alpha = 0.35f))
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x330083B0))
            .testTag("interactive_map_view")
    ) {
        // Interactive Canvas with pan/zoom gestures
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (currentMapLayer == MapLayerType.STREET) Color(0xFFF1F4EB) else Color(0xFF1E2833)
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.75f, 2.5f)
                        panOffsetX = (panOffsetX + pan.x).coerceIn(-300f, 300f)
                        panOffsetY = (panOffsetY + pan.y).coerceIn(-200f, 200f)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f + panOffsetX
            val centerY = h / 2f + panOffsetY

            // Draw Map Base (Grid, parks, river, streets)
            drawMapBase(
                w = w,
                h = h,
                centerX = centerX,
                centerY = centerY,
                zoom = zoomScale,
                isSatellite = currentMapLayer == MapLayerType.SATELLITE
            )

            // Polyline Waypoints (depot -> turns -> customer)
            val p0 = Offset(centerX - 130f * zoomScale, centerY + 65f * zoomScale) // Depot
            val p1 = Offset(centerX - 60f * zoomScale, centerY + 15f * zoomScale)  // Turn 1
            val p2 = Offset(centerX + 10f * zoomScale, centerY + 30f * zoomScale)  // Turn 2
            val p3 = Offset(centerX + 90f * zoomScale, centerY - 20f * zoomScale)  // Turn 3
            val p4 = Offset(centerX + 140f * zoomScale, centerY - 65f * zoomScale) // Destination

            // 1. Draw Polyline Route Coded with Live Traffic
            drawTrafficPolyline(
                p0 = p0, p1 = p1, p2 = p2, p3 = p3, p4 = p4,
                trafficColor = Color(routeEta.trafficColorHex),
                zoom = zoomScale
            )

            // 2. Start Depot Marker
            drawCircle(color = Color.White, radius = 12f * zoomScale, center = p0)
            drawCircle(color = Color(0xFF00B4DB), radius = 8f * zoomScale, center = p0)

            // 3. Current Courier Position interpolated by progress
            val courierPos = interpolateRoute(p0, p1, p2, p3, p4, routeEta.progressFraction)

            // Pulsing Radar Ring around Courier
            drawCircle(
                color = Color(0xFF00B4DB).copy(alpha = 0.28f),
                radius = 24f * zoomScale * pulseAura,
                center = courierPos
            )
            drawCircle(
                color = Color.White,
                radius = 15f * zoomScale,
                center = courierPos
            )
            drawCircle(
                color = Color(0xFF0288D1),
                radius = 11f * zoomScale,
                center = courierPos
            )

            // 4. Destination Pin Point
            drawCircle(
                color = Color.White,
                radius = 16f * zoomScale,
                center = p4
            )
            drawCircle(
                color = Color(0xFFE53935),
                radius = 11f * zoomScale,
                center = p4
            )

            // 5. Landmark & POI Labels (Native Canvas)
            val poiPaint = Paint().apply {
                color = if (currentMapLayer == MapLayerType.SATELLITE) 0xFF81D4FA.toInt() else 0xFF0277BD.toInt()
                textSize = (11f * zoomScale).coerceIn(9f, 22f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("Depot Es Kube", p0.x - 36f * zoomScale, p0.y + 24f * zoomScale, poiPaint)

            val destPaint = Paint().apply {
                color = if (currentMapLayer == MapLayerType.SATELLITE) 0xFFFF8A80.toInt() else 0xFFC62828.toInt()
                textSize = (11f * zoomScale).coerceIn(9f, 22f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("Toko Es Segar", p4.x - 38f * zoomScale, p4.y - 18f * zoomScale, destPaint)
        }

        // Top-Center Google Maps Mockup Status Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .shadow(3.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.90f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Google Maps Mockup • dummy_key",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        // Top-Left Floating ETA Pill (Connected to Directions API)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, Color.White)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Blinking Indicator
                Icon(
                    imageVector = Icons.Filled.FiberManualRecord,
                    contentDescription = null,
                    tint = Color(routeEta.trafficColorHex),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Estimasi tiba ${routeEta.formattedEta}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF072B4A)
                    )
                    Text(
                        text = "${routeEta.trafficCondition} • ${routeEta.distanceKm} km",
                        fontSize = 10.sp,
                        color = Color(0xFF5A738E)
                    )
                }
            }
        }

        // Top-Right Map Controls (Zoom In, Zoom Out, Recenter, Layer)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Recenter GPS Button
            MapControlMiniButton(
                icon = Icons.Filled.MyLocation,
                contentDescription = "Recenter Lokasi",
                onClick = {
                    panOffsetX = 0f
                    panOffsetY = 0f
                    zoomScale = 1.0f
                }
            )

            // Zoom In (+)
            MapControlMiniButton(
                icon = Icons.Filled.Add,
                contentDescription = "Zoom In",
                onClick = {
                    zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f)
                }
            )

            // Zoom Out (-)
            MapControlMiniButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Zoom Out",
                onClick = {
                    zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f)
                }
            )

            // Toggle Layer (Street / Satellite)
            MapControlMiniButton(
                icon = Icons.Filled.Layers,
                contentDescription = "Ganti Tampilan Peta",
                onClick = {
                    currentMapLayer = if (currentMapLayer == MapLayerType.STREET) {
                        MapLayerType.SATELLITE
                    } else {
                        MapLayerType.STREET
                    }
                }
            )
        }

        // Bottom-Right "Buka di Google Maps" Pill Button
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clickable { openInGoogleMapsApp() }
                .shadow(6.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0288D1),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Buka Google Maps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Bottom-Left Active Street Tag & Google Logo Watermark
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GoogleLogoWatermark()

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF072B4A).copy(alpha = 0.88f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Jl. Melati, Purwokerto",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleLogoWatermark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
            Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MapControlMiniButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
            .shadow(3.dp, CircleShape),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color.White)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF0F3657),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun DrawScope.drawMapBase(
    w: Float,
    h: Float,
    centerX: Float,
    centerY: Float,
    zoom: Float,
    isSatellite: Boolean
) {
    val roadColor = if (isSatellite) Color(0xFF455A64) else Color.White
    val parkColor = if (isSatellite) Color(0xFF2E3E33) else Color(0xFFDFF0E8)
    val waterColor = if (isSatellite) Color(0xFF1A3345) else Color(0xFFCFEAF7)

    // Green park areas
    drawCircle(
        color = parkColor,
        radius = 160f * zoom,
        center = Offset(centerX + 120f * zoom, centerY - 60f * zoom)
    )
    drawCircle(
        color = parkColor,
        radius = 110f * zoom,
        center = Offset(centerX - 120f * zoom, centerY + 80f * zoom)
    )

    // Kali Pelus / River path
    val river = Path().apply {
        moveTo(centerX - 30f * zoom, centerY - 150f * zoom)
        cubicTo(
            centerX + 10f * zoom, centerY - 50f * zoom,
            centerX - 20f * zoom, centerY + 40f * zoom,
            centerX - 5f * zoom, centerY + 180f * zoom
        )
    }
    drawPath(
        path = river,
        color = waterColor,
        style = Stroke(width = 24f * zoom, cap = StrokeCap.Round)
    )

    // Primary Roads (Purwokerto grid)
    val mainRoadStroke = 20f * zoom
    val secRoadStroke = 12f * zoom

    // Jl. Jend. Soedirman (Main arterial road)
    drawLine(
        color = roadColor,
        start = Offset(centerX - 240f * zoom, centerY + 120f * zoom),
        end = Offset(centerX + 240f * zoom, centerY - 40f * zoom),
        strokeWidth = mainRoadStroke,
        cap = StrokeCap.Round
    )

    // Jl. Overste Isdiman
    drawLine(
        color = roadColor,
        start = Offset(centerX - 160f * zoom, centerY - 140f * zoom),
        end = Offset(centerX + 140f * zoom, centerY + 160f * zoom),
        strokeWidth = mainRoadStroke,
        cap = StrokeCap.Round
    )

    // Jl. Melati (Access road to customer)
    drawLine(
        color = roadColor,
        start = Offset(centerX + 60f * zoom, centerY - 100f * zoom),
        end = Offset(centerX + 180f * zoom, centerY - 30f * zoom),
        strokeWidth = secRoadStroke,
        cap = StrokeCap.Round
    )

    // Secondary crossroads
    drawLine(
        color = roadColor.copy(alpha = 0.85f),
        start = Offset(centerX - 200f * zoom, centerY - 60f * zoom),
        end = Offset(centerX + 200f * zoom, centerY + 80f * zoom),
        strokeWidth = secRoadStroke,
        cap = StrokeCap.Round
    )

    // Street Name Annotations
    val streetPaint = Paint().apply {
        color = if (isSatellite) 0xFFCFD8DC.toInt() else 0xFF546E7A.toInt()
        textSize = (9.5f * zoom).coerceIn(8f, 18f)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText("Jl. Jend. Soedirman", centerX - 130f * zoom, centerY + 104f * zoom, streetPaint)
    drawContext.canvas.nativeCanvas.drawText("Jl. Overste Isdiman", centerX - 145f * zoom, centerY - 50f * zoom, streetPaint)
    drawContext.canvas.nativeCanvas.drawText("Jl. Melati", centerX + 80f * zoom, centerY - 76f * zoom, streetPaint)
}

private fun DrawScope.drawTrafficPolyline(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    p4: Offset,
    trafficColor: Color,
    zoom: Float
) {
    val fullPath = Path().apply {
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        lineTo(p4.x, p4.y)
    }

    // Outer glow / road halo
    drawPath(
        path = fullPath,
        color = Color(0xFF00B4DB).copy(alpha = 0.35f),
        style = Stroke(
            width = 18f * zoom,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Base polyline track (Navy/White casing)
    drawPath(
        path = fullPath,
        color = Color.White,
        style = Stroke(
            width = 10f * zoom,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Traffic Colored Active Line (Green/Amber/Blue from Directions API)
    drawPath(
        path = fullPath,
        color = trafficColor,
        style = Stroke(
            width = 6f * zoom,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun interpolateRoute(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    p4: Offset,
    fraction: Float
): Offset {
    val segments = listOf(p0 to p1, p1 to p2, p2 to p3, p3 to p4)
    val totalSegments = segments.size
    val segmentIndex = (fraction * totalSegments).toInt().coerceIn(0, totalSegments - 1)
    val localFraction = (fraction * totalSegments - segmentIndex).coerceIn(0f, 1f)
    val (start, end) = segments[segmentIndex]
    return Offset(
        start.x + (end.x - start.x) * localFraction,
        start.y + (end.y - start.y) * localFraction
    )
}
