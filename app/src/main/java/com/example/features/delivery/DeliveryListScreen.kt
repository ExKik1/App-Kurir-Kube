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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.core.components.StatusBadge
import com.example.core.components.TopCourierBar
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.network.CourierSupabaseService
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

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
    onNavigateToProfile: () -> Unit
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

    LiquidGlassBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("delivery_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp)
        ) {
            item {
                TopCourierBar(
                    name = courier.name,
                    statusSubtitle = "Bertugas",
                    isOnDuty = courier.isActiveDuty,
                    textColor = KubeTextDark,
                    onAvatarClick = onNavigateToProfile
                )

                Spacer(modifier = Modifier.height(6.dp))

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
                        text = "Kamis, 29 Oktober 2026 · Hari Ini",
                        fontSize = 13.sp,
                        color = KubeTextMuted
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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "$historyCount Selesai",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rekap seluruh pesanan yang telah berhasil terkirim atau retur hari ini",
                            fontSize = 12.sp,
                            color = KubeTextMuted
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
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF00B4DB).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF00B4DB).copy(alpha = 0.40f))
                                ) {
                                    Text(
                                        text = "${displayedActive.size} Aktif",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0288D1),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
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
                                onContinueDelivery = { onSelectOrder(activeOrder.id) },
                                onOpenNavigation = {
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(activeOrder.address + ", Purwokerto")}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(activeOrder.address)}"))
                                        context.startActivity(webIntent)
                                    }
                                },
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0288D1).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF0288D1).copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "$queueCount Tugas Baru",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0288D1),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
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
                                onClick = { onSelectOrder(order.id) },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .shadow(if (isSelected) 4.dp else 2.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x1A0083B0))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF00B4DB) else Color.White.copy(alpha = 0.85f)
                ),
                RoundedCornerShape(20.dp)
            )
            .testTag("filter_chip_$label"),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF0288D1) else Color.White.copy(alpha = 0.50f)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF0F3657),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ActiveDeliveryGlassCard(
    order: DeliveryOrder,
    onContinueDelivery: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.testTag("active_delivery_card_${order.id}"),
        shape = RoundedCornerShape(26.dp),
        elevation = 8.dp,
        fillAlphaTop = 0.58f,
        fillAlphaBottom = 0.32f,
        borderAlphaTop = 0.95f,
        borderAlphaBottom = 0.40f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Order No and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOMOR PESANAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6B8B9E)
                    )
                    Text(
                        text = "#${order.id}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = KubeTextDark
                    )
                }

                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Destination Store Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.80f))
                        .border(1.dp, Color.White, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = "Toko",
                        tint = KubeBlueBright,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = order.address,
                        fontSize = 12.sp,
                        color = KubeTextMuted
                    )
                    Text(
                        text = order.subDistrict,
                        fontSize = 11.sp,
                        color = Color(0xFF7E97A6)
                    )
                }

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

            // Detail boxes: Jumlah barang & Pembayaran with glass styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.70f))
                        .border(1.dp, Color.White, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Jumlah barang",
                            fontSize = 11.sp,
                            color = Color(0xFF7E97A6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = order.itemDescription,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.70f))
                        .border(1.dp, Color.White, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Pembayaran",
                            fontSize = 11.sp,
                            color = Color(0xFF7E97A6)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = order.paymentStatus,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tujuan pengiriman banner with navigation intent
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White, RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenNavigation),
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Navigasi",
                        tint = KubeBlueBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Tujuan pengiriman",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Text(
                            text = "Buka navigasi rute menuju pelanggan",
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )
                    }
                }
            }

            if (order.status == DeliveryStatus.PENDING_VERIFICATION && order.proofOfDelivery != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF3E0),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Text(
                        text = "PoD Terkirim • Penerima: ${order.proofOfDelivery.recipientName} (${order.proofOfDelivery.confirmedQuantity} unit)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action Button: Dynamic depending on status
            val buttonText = when (order.status) {
                DeliveryStatus.PENDING_VERIFICATION -> "Menunggu Verifikasi Admin"
                DeliveryStatus.ARRIVED -> "Tiba di Lokasi (Isi PoD)"
                else -> "Lanjutkan Pengiriman"
            }
            val buttonColor = when (order.status) {
                DeliveryStatus.PENDING_VERIFICATION -> Color(0xFFF57C00)
                else -> Color(0xFF0288D1)
            }

            Button(
                onClick = onContinueDelivery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("continue_delivery_button_${order.id}"),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QueueOrderGlassCard(
    order: DeliveryOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier.testTag("queue_card_${order.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = 4.dp,
        fillAlphaTop = 0.50f,
        fillAlphaBottom = 0.25f,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.35f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .border(1.dp, Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Inbox,
                    contentDescription = "Paket",
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${order.id} · ${order.customerName}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${order.itemDescription} (${order.paymentStatus})",
                    fontSize = 12.sp,
                    color = Color(0xFF537389)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Menunggu konfirmasi kurir untuk diantar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE65100)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Detail",
                tint = Color(0xFF7E97A6),
                modifier = Modifier.size(16.dp)
            )
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

    LiquidGlassCard(
        modifier = modifier.testTag("history_card_${order.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = 3.dp,
        fillAlphaTop = 0.45f,
        fillAlphaBottom = 0.22f,
        borderAlphaTop = 0.85f,
        borderAlphaBottom = 0.30f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDelivered) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                    .border(
                        1.dp,
                        if (isDelivered) Color(0xFF81C784) else Color(0xFFE57373),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDelivered) Icons.Filled.CheckCircle else Icons.Filled.HighlightOff,
                    contentDescription = if (isDelivered) "Terkirim" else "Retur",
                    tint = if (isDelivered) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "#${order.id} · ${order.customerName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = order.deliveredTime ?: "Hari ini",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF7E97A6)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${order.address} · ${order.itemDescription}",
                    fontSize = 12.sp,
                    color = Color(0xFF537389)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isDelivered) "Berhasil diterima (${order.paymentStatus})" else "Gagal kirim: ${order.notes ?: "Retur"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDelivered) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Detail",
                tint = Color(0xFFB0BEC5),
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
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 2.dp,
        fillAlphaTop = 0.40f,
        fillAlphaBottom = 0.20f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.DoneAll,
                contentDescription = null,
                tint = KubeBlueBright,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = KubeTextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
