package com.example.features.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.LiquidDarkGlassCard
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.core.components.TopCourierBar
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.network.DirectionsService
import com.example.core.repository.OrderRepository
import com.example.features.delivery.ProofOfDeliveryBottomSheet
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DeliveryMapScreen(
    orderId: String? = null,
    repository: OrderRepository = OrderRepository.instance,
    directionsService: DirectionsService = DirectionsService.instance,
    onNavigateBack: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val orders by repository.orders.collectAsState()
    val courier by repository.courier.collectAsState()
    val routeEta by directionsService.routeEta.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showProofOfDeliverySheet by remember { mutableStateOf(false) }

    val currentOrder = orders.find { it.id == orderId }
        ?: orders.firstOrNull { it.status == DeliveryStatus.IN_TRANSIT }
        ?: orders.firstOrNull()
        ?: DeliveryOrder(
            id = "ESB-00128",
            customerName = "Toko Es Segar",
            address = "Jl. Melati No. 12",
            itemDescription = "10 balok es"
        )

    val scrollState = rememberScrollState()

    fun openExternalNavigation() {
        val addressUri = Uri.parse("geo:0,0?q=${Uri.encode(currentOrder.address + ", Purwokerto")}")
        val mapIntent = Intent(Intent.ACTION_VIEW, addressUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(currentOrder.address)}"))
            context.startActivity(webIntent)
        }
    }

    LiquidGlassBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("map_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            // Top Bar with right badge "Dalam Perjalanan"
            TopCourierBar(
                name = courier.name,
                statusSubtitle = "Bertugas",
                isOnDuty = courier.isActiveDuty,
                showSearchAndNotif = false,
                textColor = KubeTextDark,
                rightActionContent = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.60f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.90f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalShipping,
                                contentDescription = null,
                                tint = KubeBlueBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentOrder.status.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F3657)
                            )
                        }
                    }
                },
                onAvatarClick = onNavigateToProfile
            )

            // Title
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    text = "Detail Pengiriman",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Lacak dan selesaikan pengiriman pesanan",
                    fontSize = 13.sp,
                    color = KubeTextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Order & Customer Summary Glass Card
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(26.dp),
                elevation = 8.dp,
                fillAlphaTop = 0.58f,
                fillAlphaBottom = 0.32f,
                borderAlphaTop = 0.95f,
                borderAlphaBottom = 0.40f
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "No. Pesanan",
                                fontSize = 11.sp,
                                color = KubeTextMuted
                            )
                            Text(
                                text = "#${currentOrder.id}",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = KubeTextDark
                            )
                        }

                        // ETA Pill with Live Real-time Traffic Indicator
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE1F5FE).copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = Color(routeEta.trafficColorHex),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Estimasi tiba\n${routeEta.formattedEta}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0277BD),
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Customer Store Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                                .border(1.dp, Color.White, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Store,
                                contentDescription = "Toko",
                                tint = KubeBlueBright,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentOrder.customerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeTextDark
                            )
                            Text(
                                text = currentOrder.phoneNumber,
                                fontSize = 12.sp,
                                color = KubeBlueBright,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${currentOrder.address}, ${currentOrder.subDistrict}",
                                fontSize = 11.sp,
                                color = KubeTextMuted,
                                maxLines = 1
                            )
                        }

                        // "Buka Navigasi" Blue Pill Button
                        Button(
                            onClick = { openExternalNavigation() },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0288D1),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("open_navigation_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka Navigasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Google Maps SDK View with Polyline and Live Traffic
            InteractiveRouteMapView(
                routeEta = routeEta,
                modifier = Modifier.padding(horizontal = 20.dp),
                onMapClick = { openExternalNavigation() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Traffic Condition & Next Step Glass Card
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = 4.dp,
                fillAlphaTop = 0.45f,
                fillAlphaBottom = 0.22f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(routeEta.trafficColorHex).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color(routeEta.trafficColorHex),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = routeEta.nextInstruction,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeTextDark
                            )
                            Text(
                                text = "${routeEta.trafficCondition} • ${routeEta.lastUpdatedText}",
                                fontSize = 10.sp,
                                color = KubeTextMuted
                            )
                        }
                    }

                    // Live Speed Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.70f),
                        border = BorderStroke(0.8.dp, Color.White)
                    ) {
                        Text(
                            text = "${routeEta.courierSpeedKmh} km/j",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0288D1),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Detail Glass Mini Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailGlassMiniCard(
                    icon = Icons.Filled.Inbox,
                    title = "Jumlah Barang",
                    value = currentOrder.itemDescription,
                    modifier = Modifier.weight(1.1f)
                )
                DetailGlassMiniCard(
                    icon = Icons.Filled.CreditCard,
                    title = "Pembayaran",
                    value = currentOrder.paymentStatus,
                    modifier = Modifier.weight(1.1f)
                )
                DetailGlassMiniCard(
                    icon = Icons.Filled.AccessTime,
                    title = "Prioritas",
                    value = currentOrder.priority,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeline: Status Pengiriman with Liquid Dark Glass
            LiquidDarkGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status Pengiriman",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = KubeBlueBright,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentOrder.distanceKm} km jarak tersisa",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timeline Steps
                    TimelineStep(
                        isCompleted = true,
                        isActive = false,
                        icon = Icons.Filled.Check,
                        title = "Tugas Diambil",
                        subtitle = currentOrder.assignedTime,
                        isLast = false
                    )

                    TimelineStep(
                        isCompleted = currentOrder.status == DeliveryStatus.ARRIVED || currentOrder.status == DeliveryStatus.PENDING_VERIFICATION || currentOrder.status == DeliveryStatus.DELIVERED,
                        isActive = currentOrder.status == DeliveryStatus.IN_TRANSIT,
                        icon = Icons.Filled.LocalShipping,
                        title = "Dalam Perjalanan",
                        subtitle = "Menuju lokasi pelanggan\n${currentOrder.inTransitTime ?: "09:45 | 28 Okt 2026"}",
                        isLast = false
                    )

                    TimelineStep(
                        isCompleted = currentOrder.status == DeliveryStatus.DELIVERED,
                        isActive = currentOrder.status == DeliveryStatus.ARRIVED || currentOrder.status == DeliveryStatus.PENDING_VERIFICATION,
                        icon = if (currentOrder.status == DeliveryStatus.PENDING_VERIFICATION) Icons.Filled.AccessTime else Icons.Filled.CheckCircle,
                        title = if (currentOrder.status == DeliveryStatus.PENDING_VERIFICATION) "Menunggu Verifikasi Admin" else "Terkirim",
                        subtitle = when (currentOrder.status) {
                            DeliveryStatus.DELIVERED -> "Telah diverifikasi dan selesai\n${currentOrder.deliveredTime ?: "10:10 | 28 Okt 2026"}"
                            DeliveryStatus.PENDING_VERIFICATION -> "Laporan PoD diserahkan oleh ${currentOrder.proofOfDelivery?.recipientName ?: "Penerima"}\nMenunggu verifikasi admin Kube"
                            else -> "Menunggu serah terima es batu & isi PoD"
                        },
                        isLast = true
                    )
                }
            }

            // PoD Verification Summary Card when PENDING_VERIFICATION
            if (currentOrder.status == DeliveryStatus.PENDING_VERIFICATION && currentOrder.proofOfDelivery != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.2.dp, Color(0xFFFFB74D))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Laporan PoD Sedang Diverifikasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• Penerima: ${currentOrder.proofOfDelivery.recipientName}\n• Jumlah: ${currentOrder.proofOfDelivery.confirmedQuantity} unit dikonfirmasi\n• Catatan: ${currentOrder.proofOfDelivery.notes ?: "Tidak ada catatan"}\n• Waktu Submit: ${currentOrder.proofOfDelivery.submittedAt}",
                            fontSize = 11.sp,
                            color = Color(0xFF4E342E),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Dynamic status actions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                val buttonText = when (currentOrder.status) {
                    DeliveryStatus.IN_TRANSIT -> "Saya Sudah Tiba"
                    DeliveryStatus.ARRIVED -> "Konfirmasi Es Diterima (Isi PoD)"
                    DeliveryStatus.PENDING_VERIFICATION -> "Menunggu Verifikasi Admin"
                    DeliveryStatus.DELIVERED -> "Pengiriman Telah Selesai"
                    else -> "Mulai Antar Sekarang"
                }

                val buttonColor = when (currentOrder.status) {
                    DeliveryStatus.DELIVERED -> Color(0xFF2E7D32)
                    DeliveryStatus.PENDING_VERIFICATION -> Color(0xFFF57C00)
                    else -> Color(0xFF0096C7)
                }

                Button(
                    onClick = {
                        when (currentOrder.status) {
                            DeliveryStatus.IN_TRANSIT -> {
                                repository.updateOrderStatus(currentOrder.id, DeliveryStatus.ARRIVED)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Status diperbarui: Anda telah tiba di lokasi pelanggan!")
                                }
                            }
                            DeliveryStatus.ARRIVED -> {
                                showProofOfDeliverySheet = true
                            }
                            DeliveryStatus.PENDING_VERIFICATION -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Data pesanan #${currentOrder.id} sedang diverifikasi oleh admin Kube.")
                                }
                            }
                            DeliveryStatus.NEW -> {
                                repository.updateOrderStatus(currentOrder.id, DeliveryStatus.IN_TRANSIT)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Pesanan #${currentOrder.id} sekarang dalam perjalanan.")
                                }
                            }
                            DeliveryStatus.DELIVERED -> {
                                onNavigateBack()
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("action_arrival_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (currentOrder.status) {
                                DeliveryStatus.PENDING_VERIFICATION -> Icons.Filled.AccessTime
                                else -> Icons.Filled.CheckCircle
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buttonText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Proof of Delivery Bottom Sheet
        if (showProofOfDeliverySheet) {
            ProofOfDeliveryBottomSheet(
                order = currentOrder,
                onDismissRequest = { showProofOfDeliverySheet = false },
                onSubmitProof = { recipientName, photoUri, confirmedQty, notes ->
                    repository.submitProofOfDelivery(
                        orderId = currentOrder.id,
                        recipientName = recipientName,
                        photoUri = photoUri,
                        confirmedQuantity = confirmedQty,
                        notes = notes
                    )
                    showProofOfDeliverySheet = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Laporan PoD berhasil dikirim! Data pesanan #${currentOrder.id} sedang diverifikasi admin.")
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
    }
}

@Composable
private fun DetailGlassMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = 3.dp,
        fillAlphaTop = 0.50f,
        fillAlphaBottom = 0.25f,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.35f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = Color(0xFF7E97A6),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KubeTextDark,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TimelineStep(
    isCompleted: Boolean,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isLast: Boolean
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> KubeGreen
                            isActive -> KubeBlueBright
                            else -> Color.White.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(
                            if (isCompleted) KubeGreen else Color.White.copy(alpha = 0.25f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 15.sp
            )
        }
    }
}
