package com.example.features.delivery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.core.components.DbBodyText
import com.example.core.components.DbTitleText
import com.example.core.components.InAppChatBottomSheet
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassIconBox
import com.example.core.components.KubeGlassPill
import com.example.core.components.KubeMutedText
import com.example.core.components.OffDutyWarningBanner
import com.example.core.components.TopCourierBar
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.network.CourierSupabaseService
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

/**
 * Filter tabs for Courier Delivery screen adhering strictly to the 3 absolute states:
 * - ALL: Active deliveries (max 2) + Queue (strictly NEW)
 * - QUEUE_ONLY: Queue only (strictly NEW)
 * - ACTIVE_ONLY: Currently in delivery only (IN_TRANSIT / ARRIVED)
 * - COMPLETED: Full history list only (DELIVERED / FAILED), hiding active and queue sections
 */
enum class DeliveryFilterTab(val label: String) {
    ALL("Semua"),
    QUEUE_ONLY("Tugas Baru"),
    ACTIVE_ONLY("Diantar"),
    COMPLETED("Selesai")
}

@Composable
fun DeliveryListScreen(
    repository: OrderRepository = OrderRepository.instance,
    initialFilter: DeliveryStatus? = null,
    onSelectOrder: (orderId: String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit = {}
) {
    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialOrders = remember(repository) { repository.orders.value }
    val initialCourier = remember(repository) { repository.courier.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }

    val orders by supabaseService.streamMyDeliveries(courierProfileId).collectAsState(initial = initialOrders)
    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val notifications by supabaseService.streamNotifications(courierProfileId).collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isTogglingDuty by remember { mutableStateOf(false) }
    var chatTargetOrder by remember { mutableStateOf<DeliveryOrder?>(null) }

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

    // Initialize filter tab based on initial filter if provided
    var selectedTab by remember(initialFilter) {
        mutableStateOf(
            when (initialFilter) {
                DeliveryStatus.NEW -> DeliveryFilterTab.QUEUE_ONLY
                DeliveryStatus.IN_TRANSIT, DeliveryStatus.ARRIVED, DeliveryStatus.PENDING_VERIFICATION -> DeliveryFilterTab.ACTIVE_ONLY
                DeliveryStatus.DELIVERED, DeliveryStatus.FAILED -> DeliveryFilterTab.COMPLETED
                null -> DeliveryFilterTab.ALL
            }
        )
    }

    // 1. Seksi Pengiriman Aktif: HANYA maksimal 1 atau 2 pesanan berstatus aktif
    val activeOrders = remember(orders) {
        orders.filter { it.isAktif }.take(2)
    }
    val allActiveOrders = remember(orders) {
        orders.filter { it.isAktif }
    }

    // 2. Seksi Antrean Berikutnya: HANYA pesanan berstatus antrean (NEW). Terkirim & Gagal 100% difilter-out
    val queueOrders = remember(orders) {
        orders.filter { it.isAntrean }
    }

    // 3. Tab Selesai: HANYA riwayat (DELIVERED & FAILED)
    val historyOrders = remember(orders) {
        orders.filter { it.isRiwayat }
    }

    val activeCount = allActiveOrders.size
    val queueCount = queueOrders.size
    val historyCount = historyOrders.size
    val activeAndQueueCount = activeCount + queueCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("delivery_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                TopCourierBar(
                    name = courier.name,
                    statusSubtitle = if (courier.isActiveDuty) "Selamat Bertugas" else "Sedang Istirahat (Off-Duty)",
                    isOnDuty = courier.isActiveDuty,
                    textColor = KubeTextDark,
                    onAvatarClick = onNavigateToProfile,
                    onToggleDutyClick = toggleDutyAction,
                    onNotifClick = onNavigateToNotifications
                )

                if (!courier.isActiveDuty) {
                    OffDutyWarningBanner(
                        onActivateOnDuty = toggleDutyAction,
                        isToggling = isTogglingDuty,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Title & Date Header
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Courier Delivery",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tugas Pengiriman & Antrean Kurir · Hari Ini",
                        fontSize = 13.sp,
                        color = Color(0xFF1E3A4C)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Frosted Glass Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassFilterChip(
                        label = "Semua ($activeAndQueueCount)",
                        isSelected = selectedTab == DeliveryFilterTab.ALL,
                        onClick = { selectedTab = DeliveryFilterTab.ALL }
                    )
                    GlassFilterChip(
                        label = "Tugas Baru ($queueCount)",
                        isSelected = selectedTab == DeliveryFilterTab.QUEUE_ONLY,
                        onClick = { selectedTab = DeliveryFilterTab.QUEUE_ONLY }
                    )
                    GlassFilterChip(
                        label = "Diantar ($activeCount)",
                        isSelected = selectedTab == DeliveryFilterTab.ACTIVE_ONLY,
                        onClick = { selectedTab = DeliveryFilterTab.ACTIVE_ONLY }
                    )
                    GlassFilterChip(
                        label = "Selesai ($historyCount)",
                        isSelected = selectedTab == DeliveryFilterTab.COMPLETED,
                        onClick = { selectedTab = DeliveryFilterTab.COMPLETED }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ATURAN 4: Pemisahan Tab Selesai
            // Jika tab 'Selesai' ditekan, SEMBUNYIKAN seksi 'Pengiriman Aktif' dan 'Antrean Berikutnya',
            // lalu tampilkan daftar riwayat penuh.
            if (selectedTab == DeliveryFilterTab.COMPLETED) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Riwayat Pengiriman",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeTextDark
                            )
                            KubeGlassPill(
                                text = "$historyCount Selesai",
                                textColor = Color(0xFF2E7D32)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rekap seluruh pesanan yang telah berhasil terkirim atau retur hari ini",
                            fontSize = 12.sp,
                            color = Color(0xFF1E3A4C)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (historyOrders.isEmpty()) {
                    item {
                        EmptyStateGlassCard(
                            message = "Belum ada pesanan yang selesai untuk hari ini.",
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    items(historyOrders, key = { it.id }) { order ->
                        HistoryOrderGlassCard(
                            order = order,
                            onClick = { onSelectOrder(order.id) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                // TAMPILAN UTAMA (Semua / Diantar / Tugas Baru)

                // ATURAN 2: Seksi 'Pengiriman Aktif'
                // Tampilkan jika tab ALL atau ACTIVE_ONLY.
                // HANYA menampilkan maksimal 1 atau 2 pesanan yang berstatus aktif (take(2)).
                if (selectedTab == DeliveryFilterTab.ALL || selectedTab == DeliveryFilterTab.ACTIVE_ONLY) {
                    val displayedActive = if (selectedTab == DeliveryFilterTab.ALL) activeOrders else allActiveOrders

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Pengiriman Aktif",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KubeTextDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                KubeGlassPill(
                                    text = "${displayedActive.size} Aktif",
                                    textColor = Color(0xFF0288D1)
                                )
                            }

                            if (selectedTab == DeliveryFilterTab.ALL && allActiveOrders.size > 2) {
                                Text(
                                    text = "Lihat semua (${allActiveOrders.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KubeBlueBright,
                                    modifier = Modifier.clickable { selectedTab = DeliveryFilterTab.ACTIVE_ONLY }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (displayedActive.isEmpty()) {
                        item {
                            EmptyStateGlassCard(
                                message = "Tidak ada pengiriman aktif saat ini. Silakan ambil pesanan dari antrean.",
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    } else {
                        items(displayedActive, key = { it.id }) { activeOrder ->
                            ActiveDeliveryGlassCard(
                                order = activeOrder,
                                isOnDuty = courier.isActiveDuty,
                                onContinueDelivery = { onSelectOrder(activeOrder.id) },
                                onOpenNavigation = {
                                    val meta = supabaseService.getOrderMeta(activeOrder.id)
                                    val lat = meta?.latitude
                                    val lng = meta?.longitude
                                    val encodedAddr = Uri.encode(activeOrder.address)
                                    val gmmIntentUri = if (lat != null && lng != null) {
                                        val label = Uri.encode("${activeOrder.customerName} (${activeOrder.address})")
                                        Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                                    } else {
                                        Uri.parse("geo:0,0?q=$encodedAddr")
                                    }
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
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
                                },
                                onChatCustomer = { chatTargetOrder = activeOrder },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }
                }

                // ATURAN 3: Seksi 'Antrean Berikutnya'
                // Tampilkan jika tab ALL atau QUEUE_ONLY.
                // HANYA memuat pesanan dengan status antrean (NEW).
                // Pesanan yang sudah selesai (Terkirim) atau gagal WAJIB disaring keluar.
                if (selectedTab == DeliveryFilterTab.ALL || selectedTab == DeliveryFilterTab.QUEUE_ONLY) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Antrean Berikutnya",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeTextDark
                            )
                            KubeGlassPill(
                                text = "$queueCount Tugas Baru",
                                textColor = Color(0xFF0288D1)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (queueOrders.isEmpty()) {
                        item {
                            EmptyStateGlassCard(
                                message = "Semua antrean tugas baru telah diambil atau selesai.",
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    } else {
                        items(queueOrders, key = { it.id }) { order ->
                            QueueOrderGlassCard(
                                order = order,
                                isOnDuty = courier.isActiveDuty,
                                onClick = { onSelectOrder(order.id) },
                                onChatCustomer = { chatTargetOrder = order },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        val activeChatOrder = chatTargetOrder
        if (activeChatOrder != null && (
                activeChatOrder.status == DeliveryStatus.NEW ||
                    activeChatOrder.status == DeliveryStatus.IN_TRANSIT ||
                    activeChatOrder.status == DeliveryStatus.ARRIVED
                )
        ) {
            InAppChatBottomSheet(
                title = activeChatOrder.customerName,
                subtitle = "Pesanan ${activeChatOrder.id}",
                roomType = "order_chat",
                orderCode = activeChatOrder.id,
                onDismiss = { chatTargetOrder = null }
            )
        }
    }
}

@Composable
private fun GlassFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (isSelected) {
        Box(
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1A0083B0))
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0288D1))
                .border(BorderStroke(1.dp, Color(0xFF67E8F9).copy(alpha = 0.85f)), RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("filter_chip_$label"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    } else {
        KubeGlassCard(
            modifier = Modifier.testTag("filter_chip_$label"),
            shape = RoundedCornerShape(20.dp),
            isHighlighted = false,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            onClick = onClick
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F3657)
            )
        }
    }
}

@Composable
private fun ActiveDeliveryGlassCard(
    order: DeliveryOrder,
    isOnDuty: Boolean,
    onContinueDelivery: () -> Unit,
    onOpenNavigation: () -> Unit,
    onChatCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInTransit = order.status == DeliveryStatus.IN_TRANSIT
    val statusTextColor = when (order.status) {
        DeliveryStatus.NEW -> Color(0xFF0288D1)
        DeliveryStatus.IN_TRANSIT -> Color(0xFF0284A8)
        DeliveryStatus.ARRIVED -> Color(0xFF2E7D32)
        DeliveryStatus.PENDING_VERIFICATION -> Color(0xFFE65100)
        DeliveryStatus.DELIVERED -> Color(0xFF059669)
        DeliveryStatus.FAILED -> Color(0xFFDC2626)
    }

    KubeGlassCard(
        modifier = modifier.testTag("active_delivery_card_${order.id}"),
        shape = RoundedCornerShape(26.dp),
        isHighlighted = isInTransit,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Order No and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOMOR PESANAN",
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
                            text = "#${order.id}",
                            textColor = Color(0xFF0284C7)
                        )
                        KubeGlassPill(
                            text = if (order.isCod) "COD" else order.paymentMethod.ifBlank { "Non-Tunai" }.uppercase(),
                            textColor = if (order.isCod) Color(0xFFB45309) else Color(0xFF1B5E20)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                KubeGlassPill(
                    text = order.status.displayName,
                    textColor = statusTextColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Destination Store Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                KubeGlassIconBox(
                    icon = Icons.Filled.Store,
                    contentDescription = "Toko",
                    tint = KubeBlueBright,
                    isHighlighted = isInTransit,
                    boxSize = 46.dp,
                    iconSize = 22.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    DbTitleText(
                        text = order.customerName,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    DbBodyText(
                        text = order.address,
                        maxLines = 2,
                        color = Color(0xFF1E3A4C)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = order.subDistrict,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3A4C)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Pin Lokasi",
                    tint = KubeBlueBright,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onOpenNavigation)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detail boxes: Jumlah barang & Pembayaran with KubeGlassCard
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KubeGlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    isHighlighted = false,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Jumlah barang",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A4C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DbTitleText(
                            text = order.itemDescription,
                            maxLines = 2
                        )
                    }
                }

                KubeGlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    isHighlighted = false,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Pembayaran",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A4C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        DbTitleText(
                            text = order.paymentStatus,
                            maxLines = 2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isChatAllowed = order.status == DeliveryStatus.NEW ||
                order.status == DeliveryStatus.IN_TRANSIT ||
                order.status == DeliveryStatus.ARRIVED

            // Tujuan pengiriman & Chat Pelanggan (khusus pesanan aktif)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KubeGlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    isHighlighted = false,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    onClick = onOpenNavigation
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Navigasi",
                            tint = KubeBlueBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Buka Rute",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }
                }

                if (isChatAllowed) {
                    OutlinedButton(
                        onClick = onChatCustomer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, Color(0xFF0083B0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.72f),
                            contentColor = Color(0xFF0083B0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_chat_customer_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat Pelanggan",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat Pelanggan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (order.status == DeliveryStatus.PENDING_VERIFICATION && order.proofOfDelivery != null) {
                KubeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isHighlighted = false,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "PoD Terkirim • Penerima: ${order.proofOfDelivery.recipientName} (${order.proofOfDelivery.confirmedQuantity} unit)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action Button: Dynamic depending on status & Off-Duty lock
            val buttonText = if (!isOnDuty) {
                "Terkunci (Status Off-Duty)"
            } else {
                when (order.status) {
                    DeliveryStatus.PENDING_VERIFICATION -> "Menunggu Verifikasi Admin"
                    DeliveryStatus.ARRIVED -> "Tiba di Lokasi (Isi PoD)"
                    else -> "Lanjutkan Pengiriman"
                }
            }
            val buttonColor = when (order.status) {
                DeliveryStatus.PENDING_VERIFICATION -> Color(0xFFF57C00)
                else -> Color(0xFF0288D1)
            }

            Button(
                onClick = onContinueDelivery,
                enabled = isOnDuty,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("continue_delivery_button_${order.id}"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF94A3B8).copy(alpha = 0.55f),
                    disabledContentColor = Color.White.copy(alpha = 0.90f)
                )
            ) {
                if (!isOnDuty) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = buttonText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QueueOrderGlassCard(
    order: DeliveryOrder,
    isOnDuty: Boolean,
    onClick: () -> Unit,
    onChatCustomer: () -> Unit,
    modifier: Modifier = Modifier
) {
    KubeGlassCard(
        modifier = modifier.testTag("queue_card_${order.id}"),
        shape = RoundedCornerShape(20.dp),
        isHighlighted = false,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KubeGlassIconBox(
                    icon = Icons.Filled.Inbox,
                    contentDescription = "Paket",
                    tint = Color(0xFF0288D1),
                    isHighlighted = false,
                    boxSize = 44.dp,
                    iconSize = 22.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KubeGlassPill(
                            text = "#${order.id}",
                            textColor = Color(0xFF0284C7)
                        )
                        KubeGlassPill(
                            text = if (order.isCod) "COD" else order.paymentMethod.ifBlank { "Non-Tunai" }.uppercase(),
                            textColor = if (order.isCod) Color(0xFFB45309) else Color(0xFF1B5E20)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    DbTitleText(
                        text = order.customerName,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    DbBodyText(
                        text = "${order.itemDescription} · ${order.paymentStatus}",
                        maxLines = 2,
                        color = Color(0xFF1E3A4C)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Menunggu konfirmasi kurir untuk diantar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Detail",
                    tint = Color(0xFF1E3A4C),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (order.status == DeliveryStatus.NEW ||
                    order.status == DeliveryStatus.IN_TRANSIT ||
                    order.status == DeliveryStatus.ARRIVED
                ) {
                    OutlinedButton(
                        onClick = onChatCustomer,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFF0083B0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.72f),
                            contentColor = Color(0xFF0083B0)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_chat_customer_queue_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat Pelanggan",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onClick,
                    enabled = isOnDuty,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF94A3B8).copy(alpha = 0.55f),
                        disabledContentColor = Color.White.copy(alpha = 0.90f)
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .heightIn(min = 48.dp)
                        .testTag("btn_start_queue_${order.id}")
                ) {
                    if (!isOnDuty) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = if (isOnDuty) "Ambil / Mulai Pengantaran" else "Terkunci (Status Off-Duty)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryOrderGlassCard(
    order: DeliveryOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDelivered = order.status == DeliveryStatus.DELIVERED
    val statusColor = if (isDelivered) Color(0xFF1B5E20) else Color(0xFFC62828)

    KubeGlassCard(
        modifier = modifier.testTag("history_card_${order.id}"),
        shape = RoundedCornerShape(20.dp),
        isHighlighted = false,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KubeGlassIconBox(
                icon = if (isDelivered) Icons.Filled.CheckCircle else Icons.Filled.HighlightOff,
                contentDescription = if (isDelivered) "Terkirim" else "Retur",
                tint = statusColor,
                isHighlighted = false,
                boxSize = 44.dp,
                iconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KubeGlassPill(
                            text = "#${order.id}",
                            textColor = statusColor
                        )
                        KubeGlassPill(
                            text = order.status.displayName,
                            textColor = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.deliveredTime ?: "Hari ini",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E3A4C)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                DbTitleText(
                    text = order.customerName,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                DbBodyText(
                    text = "${order.address} · ${order.itemDescription}",
                    maxLines = 2,
                    color = Color(0xFF1E3A4C)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (isDelivered) "Berhasil diterima (${order.paymentStatus})" else "Gagal kirim: ${order.notes ?: "Retur"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Detail",
                tint = Color(0xFF1E3A4C),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateGlassCard(
    message: String,
    modifier: Modifier = Modifier
) {
    KubeGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        isHighlighted = false,
        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KubeGlassIconBox(
                icon = Icons.Filled.DoneAll,
                contentDescription = null,
                tint = KubeBlueBright,
                isHighlighted = false,
                boxSize = 48.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color(0xFF1E3A4C),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
