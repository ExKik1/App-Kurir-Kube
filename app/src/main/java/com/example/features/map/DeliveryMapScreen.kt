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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.DarkObsidianGlassCard
import com.example.core.components.DbBodyText
import com.example.core.components.DbCurrencyOrMetricText
import com.example.core.components.DbTitleText
import com.example.core.components.InAppChatBottomSheet
import com.example.core.components.KubeFeedbackManager
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassIconBox
import com.example.core.components.KubeGlassPill
import com.example.core.components.KubeMutedText
import com.example.core.components.KubePrimaryText
import com.example.core.components.OffDutyWarningBanner
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
    val initialCourier = remember(repository) { repository.courier.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }

    // Observasi langsung ke repository StateFlow & aktifkan realtime sync stream
    val orders by repository.orders.collectAsState()
    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val routeEta by directionsService.routeEta.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showProofOfDeliverySheet by remember { mutableStateOf(false) }
    var showCustomerChatSheet by remember { mutableStateOf(false) }
    var isTogglingDuty by remember { mutableStateOf(false) }

    val toggleDutyAction: () -> Unit = {
        if (!isTogglingDuty) {
            isTogglingDuty = true
            coroutineScope.launch {
                try {
                    supabaseService.toggleOnDuty(
                        courierId = courier.id,
                        isOnDuty = !courier.isActiveDuty
                    )
                } finally {
                    isTogglingDuty = false
                }
            }
        }
    }

    LaunchedEffect(courierProfileId) {
        supabaseService.getMyDeliveries(courierProfileId)
    }

    // HANYA pilih pesanan yang masih aktif (NEW / IN_TRANSIT / ARRIVED).
    // Jika pesanan menjadi DELIVERED/FAILED, otomatis beralih ke pesanan aktif lain atau kosong (null).
    val activeOrders = remember(orders) {
        orders.filter {
            it.status == DeliveryStatus.NEW ||
                it.status == DeliveryStatus.IN_TRANSIT ||
                it.status == DeliveryStatus.ARRIVED
        }
    }

    val activeOrder = remember(activeOrders, orderId) {
        activeOrders.find { it.id == orderId }
            ?: activeOrders.firstOrNull {
                it.status == DeliveryStatus.IN_TRANSIT || it.status == DeliveryStatus.ARRIVED
            }
            ?: activeOrders.firstOrNull()
    }

    val activeOrderMeta = remember(activeOrder?.id, orders) {
        activeOrder?.let { supabaseService.getOrderMeta(it.id) }
    }

    LaunchedEffect(
        activeOrder?.id,
        activeOrder?.customerName,
        activeOrder?.address,
        activeOrder?.distanceKm,
        activeOrder?.estimatedTimeMinutes,
        activeOrderMeta?.latitude,
        activeOrderMeta?.longitude
    ) {
        if (activeOrder != null) {
            directionsService.bindActiveOrder(
                destinationName = activeOrder.customerName,
                destinationAddress = activeOrder.address,
                distanceKm = activeOrder.distanceKm,
                estimatedMinutes = activeOrder.estimatedTimeMinutes,
                destLat = activeOrderMeta?.latitude,
                destLng = activeOrderMeta?.longitude
            )
        }
    }

    val scrollState = rememberScrollState()

    fun openExternalNavigation(order: DeliveryOrder, meta: OrderPaymentMeta?) {
        val lat = meta?.latitude
        val lng = meta?.longitude
        val encodedAddr = Uri.encode(order.address)
        val gmmIntentUri = if (lat != null && lng != null) {
            val label = Uri.encode("${order.customerName} (${order.address})")
            Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        } else {
            Uri.parse("geo:0,0?q=$encodedAddr")
        }
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val webUrl = if (lat != null && lng != null) {
                "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
            } else {
                "https://www.google.com/maps/search/?api=1&query=$encodedAddr"
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
        }
    }

    Box(
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
            // Top Bar
            TopCourierBar(
                name = courier.name,
                statusSubtitle = if (courier.isActiveDuty) "Selamat Bertugas" else "Sedang Istirahat (Off-Duty)",
                isOnDuty = courier.isActiveDuty,
                showNotifButton = false,
                textColor = KubePrimaryText,
                rightActionContent = {
                    KubeGlassPill(
                        text = activeOrder?.status?.displayName ?: "Tidak Ada Tugas",
                        textColor = KubePrimaryText
                    )
                },
                onAvatarClick = onNavigateToProfile,
                onToggleDutyClick = toggleDutyAction
            )

            // Title
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text(
                    text = "Detail Pengiriman",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubePrimaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                DbBodyText(
                    text = "Lacak rute pelanggan dan selesaikan pengiriman aktif",
                    maxLines = 2,
                    color = Color(0xFF1E3A4C)
                )
            }

            if (!courier.isActiveDuty) {
                Spacer(modifier = Modifier.height(10.dp))
                OffDutyWarningBanner(
                    onActivateOnDuty = toggleDutyAction,
                    isToggling = isTogglingDuty,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (activeOrder == null) {
                KubeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("empty_active_map_card"),
                    shape = RoundedCornerShape(24.dp),
                    isHighlighted = false,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KubeGlassIconBox(
                            icon = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = KubeBlueBright,
                            isHighlighted = false,
                            boxSize = 52.dp,
                            iconSize = 26.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DbTitleText(
                            text = "Tidak ada pengiriman aktif saat ini",
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DbBodyText(
                            text = "Seluruh pengiriman aktif Anda telah selesai atau belum ada tugas yang berjalan. Cek tab Delivery untuk mengambil tugas baru.",
                            maxLines = 3,
                            color = Color(0xFF1E3A4C)
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
            KubeGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(26.dp),
                isHighlighted = activeOrder.status == DeliveryStatus.IN_TRANSIT,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No. Pesanan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1E3A4C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                KubeGlassPill(
                                    text = "#${activeOrder.id}",
                                    textColor = Color(0xFF0284C7)
                                )
                                KubeGlassPill(
                                    text = activeOrder.status.displayName,
                                    textColor = if (activeOrder.status == DeliveryStatus.IN_TRANSIT) {
                                        Color(0xFF0284A8)
                                    } else {
                                        Color(0xFF0F172A)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        KubeGlassPill(
                            text = "${activeOrder.estimatedTimeMinutes} menit",
                            textColor = Color(0xFF0277BD)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Customer Store Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KubeGlassIconBox(
                            icon = Icons.Filled.Store,
                            contentDescription = "Toko",
                            tint = KubeBlueBright,
                            isHighlighted = activeOrder.status == DeliveryStatus.IN_TRANSIT,
                            boxSize = 48.dp,
                            iconSize = 24.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            DbTitleText(
                                text = activeOrder.customerName,
                                maxLines = 1
                            )
                            if (activeOrder.phoneNumber.isNotBlank()) {
                                Text(
                                    text = activeOrder.phoneNumber,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            DbBodyText(
                                text = activeOrder.address,
                                maxLines = 2,
                                color = Color(0xFF1E3A4C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val isChatAllowed = activeOrder.status == DeliveryStatus.NEW ||
                        activeOrder.status == DeliveryStatus.IN_TRANSIT ||
                        activeOrder.status == DeliveryStatus.ARRIVED

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Buka Navigasi" Blue Pill Button
                        Button(
                            onClick = { openExternalNavigation(activeOrder, activeOrderMeta) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0288D1),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("open_navigation_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Navigasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (isChatAllowed) {
                            OutlinedButton(
                                onClick = { showCustomerChatSheet = true },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.2.dp, Color(0xFF0083B0)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.78f),
                                    contentColor = Color(0xFF0083B0)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_chat_customer_map")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Chat Pelanggan",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat Pelanggan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Map View bound to real customer address, name & coordinates
            InteractiveRouteMapView(
                routeEta = routeEta,
                destinationName = activeOrder.customerName,
                destinationAddress = activeOrder.address,
                distanceKm = activeOrder.distanceKm,
                estimatedTimeMinutes = activeOrder.estimatedTimeMinutes,
                destLat = activeOrderMeta?.latitude,
                destLng = activeOrderMeta?.longitude,
                modifier = Modifier.padding(horizontal = 20.dp),
                onMapClick = { openExternalNavigation(activeOrder, activeOrderMeta) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Route & Destination Card (100% synced with activeOrder)
            KubeGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                isHighlighted = false,
                contentPadding = PaddingValues(16.dp)
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
                        KubeGlassIconBox(
                            icon = Icons.Filled.LocationOn,
                            contentDescription = "Lokasi Tujuan",
                            tint = Color(routeEta.trafficColorHex),
                            isHighlighted = false,
                            boxSize = 40.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            DbTitleText(
                                text = "Tujuan: ${activeOrder.customerName}",
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            DbBodyText(
                                text = activeOrder.address,
                                maxLines = 2,
                                color = Color(0xFF1E3A4C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        KubeGlassPill(
                            text = "${activeOrder.distanceKm} km",
                            textColor = Color(0xFF0284C7)
                        )
                        KubeGlassPill(
                            text = "${activeOrder.estimatedTimeMinutes} menit",
                            textColor = KubePrimaryText
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
                    value = activeOrder.itemDescription,
                    modifier = Modifier.weight(1.1f)
                )
                DetailGlassMiniCard(
                    icon = Icons.Filled.CreditCard,
                    title = "Pembayaran",
                    value = activeOrder.paymentStatus,
                    modifier = Modifier.weight(1.1f)
                )
                DetailGlassMiniCard(
                    icon = Icons.Filled.AccessTime,
                    title = "Prioritas",
                    value = activeOrder.priority,
                    modifier = Modifier.weight(0.9f)
                )
            }

            val isCodOrder = activeOrderMeta?.isCodOrder ?: activeOrder.isCod

            if (isCodOrder) {
                Spacer(modifier = Modifier.height(14.dp))
                CodPaymentCollectionCard(
                    order = activeOrder,
                    orderMeta = activeOrderMeta,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeline: Status Pengiriman with Dark Obsidian Glass
            DarkObsidianGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status Pengiriman",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
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
                                text = "${activeOrder.distanceKm} km jarak tersisa",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TimelineStep(
                        isCompleted = true,
                        isActive = false,
                        icon = Icons.Filled.Check,
                        title = "Tugas Diambil",
                        subtitle = activeOrder.assignedTime,
                        isLast = false
                    )

                    TimelineStep(
                        isCompleted = activeOrder.status == DeliveryStatus.ARRIVED,
                        isActive = activeOrder.status == DeliveryStatus.IN_TRANSIT,
                        icon = Icons.Filled.LocalShipping,
                        title = "Dalam Perjalanan",
                        subtitle = "Menuju ${activeOrder.address}\n${activeOrder.inTransitTime ?: activeOrder.assignedTime}",
                        isLast = false
                    )

                    TimelineStep(
                        isCompleted = false,
                        isActive = activeOrder.status == DeliveryStatus.ARRIVED,
                        icon = Icons.Filled.CheckCircle,
                        title = "Terkirim",
                        subtitle = "Menunggu serah terima es batu & isi PoD",
                        isLast = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button: Only displayed for active orders (NEW, IN_TRANSIT, ARRIVED) & locked when Off-Duty
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                val baseActionText = when (activeOrder.status) {
                    DeliveryStatus.IN_TRANSIT -> "Tiba di Lokasi"
                    DeliveryStatus.ARRIVED -> "Pengiriman Telah Selesai (Isi PoD)"
                    else -> "Ambil / Mulai Pengantaran"
                }
                val isDutyActive = courier.isActiveDuty
                val buttonText = if (isDutyActive) baseActionText else "$baseActionText · Terkunci (Status Off-Duty)"

                Button(
                    onClick = {
                        if (!isDutyActive) return@Button
                        when (activeOrder.status) {
                            DeliveryStatus.IN_TRANSIT -> {
                                coroutineScope.launch {
                                    try {
                                        val res = CourierSupabaseService.instance.updateDeliveryProgress(
                                            activeOrder.id,
                                            DeliveryStatus.ARRIVED
                                        )
                                        res.fold(
                                            onSuccess = {
                                                KubeFeedbackManager.showInfo(
                                                    "Status Pengiriman Diperbarui",
                                                    "Pesanan #${activeOrder.id} diperbarui menjadi ${DeliveryStatus.ARRIVED.displayName}."
                                                )
                                            },
                                            onFailure = { err ->
                                                KubeFeedbackManager.showError(
                                                    "Gagal Memperbarui Status",
                                                    err.message ?: "Gagal memperbarui status pengiriman."
                                                )
                                            }
                                        )
                                    } catch (e: Exception) {
                                        KubeFeedbackManager.showError(
                                            "Gagal Memperbarui Status",
                                            e.message ?: "Terjadi kendala saat memperbarui status pengiriman."
                                        )
                                    }
                                }
                            }
                            DeliveryStatus.ARRIVED -> {
                                showProofOfDeliverySheet = true
                            }
                            DeliveryStatus.NEW -> {
                                coroutineScope.launch {
                                    try {
                                        val res = CourierSupabaseService.instance.updateDeliveryProgress(
                                            activeOrder.id,
                                            DeliveryStatus.IN_TRANSIT
                                        )
                                        res.fold(
                                            onSuccess = {
                                                KubeFeedbackManager.showInfo(
                                                    "Status Pengiriman Diperbarui",
                                                    "Pesanan #${activeOrder.id} diperbarui menjadi ${DeliveryStatus.IN_TRANSIT.displayName}."
                                                )
                                            },
                                            onFailure = { err ->
                                                KubeFeedbackManager.showError(
                                                    "Gagal Memperbarui Status",
                                                    err.message ?: "Gagal memperbarui status pengiriman."
                                                )
                                            }
                                        )
                                    } catch (e: Exception) {
                                        KubeFeedbackManager.showError(
                                            "Gagal Memperbarui Status",
                                            e.message ?: "Terjadi kendala saat memperbarui status pengiriman."
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    },
                    enabled = isDutyActive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("action_arrival_button"),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0096C7),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF94A3B8).copy(alpha = 0.55f),
                        disabledContentColor = Color.White.copy(alpha = 0.90f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isDutyActive) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buttonText,
                            fontSize = if (isDutyActive) 15.sp else 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isDutyActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Proof of Delivery Bottom Sheet
        if (showProofOfDeliverySheet && activeOrder != null) {
            ProofOfDeliveryBottomSheet(
                order = activeOrder,
                orderMeta = activeOrderMeta,
                isOnDuty = courier.isActiveDuty,
                onDismissRequest = { showProofOfDeliverySheet = false },
                onSubmitProof = { recipientName, photoUri, codCashPhotoUri, confirmedQty, notes, isCodCashConfirmed ->
                    showProofOfDeliverySheet = false
                    coroutineScope.launch {
                        try {
                            val res = supabaseService.completeDeliveryWithPoD(
                                orderId = activeOrder.id,
                                recipientName = recipientName,
                                photoUri = photoUri,
                                codCashPhotoUri = codCashPhotoUri,
                                confirmedQuantity = confirmedQty,
                                notes = notes,
                                context = context,
                                isCodCollected = isCodCashConfirmed
                            )
                            res.fold(
                                onSuccess = {
                                    KubeFeedbackManager.showSuccess(
                                        "Pengiriman Selesai",
                                        "Bukti serah terima tersimpan & komisi langsung masuk ke Saldo Siap Cair."
                                    )
                                },
                                onFailure = { err ->
                                    KubeFeedbackManager.showError(
                                        "Gagal Mengirim PoD",
                                        err.message ?: "Gagal mengirim laporan PoD. Silakan coba lagi."
                                    )
                                }
                            )
                        } catch (e: Exception) {
                            KubeFeedbackManager.showError(
                                "Gagal Mengirim PoD",
                                e.message ?: "Terjadi kendala saat menyelesaikan pengiriman."
                            )
                        }
                    }
                }
            )
        }

        // In-App Customer Chat Bottom Sheet (hanya untuk pesanan aktif NEW / IN_TRANSIT / ARRIVED)
        if (showCustomerChatSheet && activeOrder != null && (
                activeOrder.status == DeliveryStatus.NEW ||
                    activeOrder.status == DeliveryStatus.IN_TRANSIT ||
                    activeOrder.status == DeliveryStatus.ARRIVED
                )
        ) {
            InAppChatBottomSheet(
                title = activeOrder.customerName,
                subtitle = "Pesanan ${activeOrder.id}",
                roomType = "order_chat",
                orderCode = activeOrder.id,
                onDismiss = { showCustomerChatSheet = false }
            )
        }
    }
}

@Composable
private fun DetailGlassMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    KubeGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        isHighlighted = false,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            KubeGlassIconBox(
                icon = icon,
                contentDescription = title,
                tint = Color(0xFF0288D1),
                isHighlighted = false,
                boxSize = 30.dp,
                iconSize = 15.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E3A4C),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            DbTitleText(
                text = value,
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
                color = Color.White.copy(alpha = 0.78f),
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
    val dividerColor = if (isPaid) Color(0xFF81C784) else Color(0xFFFBBF24)

    KubeGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cod_payment_collection_card"),
        shape = RoundedCornerShape(22.dp),
        isHighlighted = !isPaid,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KubeGlassIconBox(
                        icon = Icons.Filled.Payments,
                        contentDescription = "Pembayaran COD",
                        tint = accentColor,
                        isHighlighted = false,
                        boxSize = 40.dp,
                        iconSize = 20.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "BAYAR DI TEMPAT (COD)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            letterSpacing = 0.4.sp
                        )
                        DbTitleText(
                            text = "Rincian Tagihan Tunai",
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                KubeGlassPill(
                    text = if (isPaid) "Lunas" else "Tagih Tunai",
                    textColor = accentColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Rincian Pesanan",
                    fontSize = 12.sp,
                    color = Color(0xFF1E3A4C)
                )
                Spacer(modifier = Modifier.width(12.dp))
                DbBodyText(
                    text = "${order.itemDescription} (${order.defaultQuantity} unit)",
                    maxLines = 2,
                    color = KubePrimaryText,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Harga Satuan Es",
                    fontSize = 12.sp,
                    color = Color(0xFF1E3A4C),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                DbCurrencyOrMetricText(
                    text = "${formatCodCardRupiah(unitPriceToShow)} / unit",
                    color = KubePrimaryText,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = dividerColor.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DbTitleText(
                    text = "Total yang Harus Ditagih",
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                DbCurrencyOrMetricText(
                    text = formatCodCardRupiah(totalAmountToShow),
                    color = accentColor,
                    fontSize = 18.sp,
                    modifier = Modifier.testTag("cod_total_amount_text")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            DbBodyText(
                text = if (isPaid) {
                    "Uang tunai COD telah dikonfirmasi diterima dan berstatus Lunas."
                } else {
                    "Pastikan Anda menerima uang tunai sesuai nominal di atas sebelum mengirim PoD."
                },
                maxLines = 2,
                color = if (isPaid) Color(0xFF1B5E20) else Color(0xFF92400E)
            )
        }
    }
}

private fun formatCodCardRupiah(amount: Long): String {
    val formatted = String.format(java.util.Locale("in", "ID"), "%,d", amount).replace(',', '.')
    return "Rp $formatted"
}
