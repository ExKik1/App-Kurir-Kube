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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.core.network.CourierSupabaseService
import com.example.core.network.DirectionsService
import com.example.core.network.OrderPaymentMeta
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
    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialOrders = remember(repository) { repository.orders.value }
    val initialCourier = remember(repository) { repository.courier.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }

    val orders by supabaseService.streamMyDeliveries(courierProfileId).collectAsState(initial = initialOrders)
    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val routeEta by directionsService.routeEta.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showProofOfDeliverySheet by remember { mutableStateOf(false) }

    val currentOrder = orders.find { it.id == orderId }
        ?: orders.firstOrNull { it.status == DeliveryStatus.IN_TRANSIT }
        ?: orders.firstOrNull()

    val scrollState = rememberScrollState()

    fun openExternalNavigation(address: String) {
        val addressUri = Uri.parse("geo:0,0?q=${Uri.encode("$address, Purwokerto")}")
        val mapIntent = Intent(Intent.ACTION_VIEW, addressUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(address)}"))
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
                                text = currentOrder?.status?.displayName ?: "Menunggu Tugas",
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

            if (currentOrder == null) {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = KubeBlueBright,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Belum Ada Pengiriman Aktif",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daftar pengiriman dari Supabase saat ini masih kosong. Silakan cek tab Delivery untuk pesanan terbaru.",
                            fontSize = 13.sp,
                            color = KubeTextMuted
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KubeBlueBright,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Lihat Daftar Delivery", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {

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
                            onClick = { openExternalNavigation(currentOrder.address) },
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
                onMapClick = { openExternalNavigation(currentOrder.address) }
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

            val orderMeta = remember(currentOrder.id, orders) {
                supabaseService.getOrderMeta(currentOrder.id)
            }
            val isCodOrder = orderMeta?.isCodOrder ?: currentOrder.isCod

            if (isCodOrder) {
                Spacer(modifier = Modifier.height(14.dp))
                CodPaymentCollectionCard(
                    order = currentOrder,
                    orderMeta = orderMeta,
                    modifier = Modifier.padding(horizontal = 20.dp)
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
                        isCompleted = currentOrder.status == DeliveryStatus.ARRIVED || currentOrder.status == DeliveryStatus.DELIVERED,
                        isActive = currentOrder.status == DeliveryStatus.IN_TRANSIT,
                        icon = Icons.Filled.LocalShipping,
                        title = "Dalam Perjalanan",
                        subtitle = "Menuju lokasi pelanggan\n${currentOrder.inTransitTime ?: "09:45 | 28 Okt 2026"}",
                        isLast = false
                    )

                    // Poin Ketiga: Terkirim langsung aktif dengan centang hijau saat PoD selesai
                    TimelineStep(
                        isCompleted = currentOrder.status == DeliveryStatus.DELIVERED,
                        isActive = currentOrder.status == DeliveryStatus.ARRIVED,
                        icon = Icons.Filled.CheckCircle,
                        title = "Terkirim",
                        subtitle = if (currentOrder.status == DeliveryStatus.DELIVERED) {
                            "Terkirim ke ${currentOrder.proofOfDelivery?.recipientName ?: currentOrder.customerName}\n${currentOrder.deliveredTime ?: "10:15 | 28 Okt 2026"}"
                        } else {
                            "Menunggu serah terima es batu & isi PoD"
                        },
                        isLast = true
                    )
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
                    DeliveryStatus.DELIVERED -> "Pengiriman Telah Selesai"
                    else -> "Mulai Antar Sekarang"
                }

                val buttonColor = when (currentOrder.status) {
                    DeliveryStatus.DELIVERED -> Color(0xFF2E7D32)
                    else -> Color(0xFF0096C7)
                }

                Button(
                    onClick = {
                        when (currentOrder.status) {
                            DeliveryStatus.IN_TRANSIT -> {
                                coroutineScope.launch {
                                    try {
                                        CourierSupabaseService.instance.updateDeliveryProgress(
                                            currentOrder.id,
                                            DeliveryStatus.ARRIVED
                                        )
                                        snackbarHostState.showSnackbar("Status diperbarui: Anda telah tiba di lokasi pelanggan!")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal memperbarui status: ${e.message ?: "Coba lagi."}")
                                    }
                                }
                            }
                            DeliveryStatus.ARRIVED -> {
                                showProofOfDeliverySheet = true
                            }
                            DeliveryStatus.NEW -> {
                                coroutineScope.launch {
                                    try {
                                        CourierSupabaseService.instance.updateDeliveryProgress(
                                            currentOrder.id,
                                            DeliveryStatus.IN_TRANSIT
                                        )
                                        snackbarHostState.showSnackbar("Pesanan #${currentOrder.id} sekarang dalam perjalanan.")
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Gagal memperbarui status: ${e.message ?: "Coba lagi."}")
                                    }
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
                            imageVector = Icons.Filled.CheckCircle,
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
        }

        // Proof of Delivery Bottom Sheet
        if (showProofOfDeliverySheet && currentOrder != null) {
            val activeOrderMeta = remember(currentOrder.id, orders) {
                supabaseService.getOrderMeta(currentOrder.id)
            }
            ProofOfDeliveryBottomSheet(
                order = currentOrder,
                orderMeta = activeOrderMeta,
                onDismissRequest = { showProofOfDeliverySheet = false },
                onSubmitProof = { recipientName, photoUri, confirmedQty, notes, isCodCashConfirmed ->
                    showProofOfDeliverySheet = false
                    coroutineScope.launch {
                        try {
                            val res = supabaseService.completeDeliveryWithPoD(
                                orderId = currentOrder.id,
                                recipientName = recipientName,
                                photoUri = photoUri,
                                confirmedQuantity = confirmedQty,
                                notes = notes,
                                context = context,
                                isCodCollected = isCodCashConfirmed
                            )
                            res.fold(
                                onSuccess = {
                                    snackbarHostState.showSnackbar(
                                        "Pengiriman berhasil diselesaikan! Pesanan #${currentOrder.id} masuk ke riwayat & komisi masuk ke Saldo Tertunda."
                                    )
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar(
                                        err.message ?: "Gagal mengirim laporan PoD. Silakan coba lagi."
                                    )
                                }
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Terjadi kendala saat menyelesaikan pengiriman.")
                        }
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

@Composable
private fun CodPaymentCollectionCard(
    order: DeliveryOrder,
    orderMeta: OrderPaymentMeta? = null,
    modifier: Modifier = Modifier
) {
    val isPaid = orderMeta?.isPaid == true ||
        order.paymentStatus.equals("Lunas", ignoreCase = true) ||
        order.paymentStatus.equals("paid", ignoreCase = true) ||
        order.paymentStatus.contains("Lunas", ignoreCase = true)
    val unitPriceToShow = orderMeta?.unitPrice?.takeIf { it > 0L } ?: order.effectiveUnitPrice
    val totalAmountToShow = orderMeta?.totalAmount?.takeIf { it > 0L } ?: order.effectiveTotalAmount
    val accentColor = if (isPaid) Color(0xFF2E7D32) else Color(0xFFD97706)
    val surfaceBg = if (isPaid) Color(0xFFE8F5E9).copy(alpha = 0.92f) else Color(0xFFFFF8E1).copy(alpha = 0.94f)
    val borderColor = if (isPaid) Color(0xFF81C784) else Color(0xFFFBBF24)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cod_payment_collection_card"),
        shape = RoundedCornerShape(22.dp),
        color = surfaceBg,
        border = BorderStroke(1.2.dp, borderColor),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = "Pembayaran COD",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "BAYAR DI TEMPAT (COD)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            letterSpacing = 0.4.sp
                        )
                        Text(
                            text = "Rincian Tagihan Tunai",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isPaid) "Lunas" else "Tagih Tunai",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rincian Pesanan",
                    fontSize = 12.sp,
                    color = KubeTextMuted
                )
                Text(
                    text = "${order.itemDescription} (${order.defaultQuantity} unit)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KubeTextDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Harga Satuan Es",
                    fontSize = 12.sp,
                    color = KubeTextMuted
                )
                Text(
                    text = "${formatCodCardRupiah(unitPriceToShow)} / unit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KubeTextDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = borderColor.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total yang Harus Ditagih",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
                Text(
                    text = formatCodCardRupiah(totalAmountToShow),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    modifier = Modifier.testTag("cod_total_amount_text")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isPaid) {
                    "Uang tunai COD telah dikonfirmasi diterima dan berstatus Lunas."
                } else {
                    "Pastikan Anda menerima uang tunai sesuai nominal di atas sebelum mengirim PoD."
                },
                fontSize = 11.sp,
                color = if (isPaid) Color(0xFF1B5E20) else Color(0xFF92400E),
                lineHeight = 15.sp
            )
        }
    }
}

private fun formatCodCardRupiah(amount: Long): String {
    val formatted = String.format(java.util.Locale("in", "ID"), "%,d", amount).replace(',', '.')
    return "Rp $formatted"
}
