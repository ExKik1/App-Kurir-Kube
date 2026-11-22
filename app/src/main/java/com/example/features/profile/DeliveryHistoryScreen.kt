package com.example.features.profile

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.DarkObsidianGlassCard
import com.example.core.components.DeliveryHistoryDetailBottomSheet
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassIconBox
import com.example.core.components.KubeGlassPill
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.network.CourierSupabaseService
import com.example.core.network.OrderPaymentMeta
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

/**
 * Screen untuk Riwayat Pengiriman Kurir.
 * HANYA mengambil dan menampilkan histori pesanan dengan status 'Selesai' (DELIVERED)
 * atau 'Gagal/Retur' (FAILED) secara real-time melalui streamMyDeliveries().
 */
@Composable
fun DeliveryHistoryScreen(
    repository: OrderRepository = OrderRepository.instance,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialOrders = remember(repository) { repository.orders.value }
    val allOrders by supabaseService.streamMyDeliveries(activeCourierProfileId.orEmpty()).collectAsState(initial = initialOrders)

    // Filter HANYA pesanan berstatus Selesai atau Gagal
    val historyOrders = remember(allOrders) {
        allOrders.filter {
            it.status == DeliveryStatus.DELIVERED || it.status == DeliveryStatus.FAILED
        }
    }

    val completedCount = historyOrders.count { it.status == DeliveryStatus.DELIVERED }
    val failedCount = historyOrders.count { it.status == DeliveryStatus.FAILED }
    var selectedDetailOrder by remember { mutableStateOf<DeliveryOrder?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("delivery_history_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KubeGlassCard(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_back_delivery_history"),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    onClick = onBack
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = KubeTextDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Riwayat Pengiriman",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "Daftar pesanan Selesai dan Retur/Gagal",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A4C)
                    )
                }
            }

            // Summary Metric Card
            DarkObsidianGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rekapitulasi Histori",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${historyOrders.size} Pengantaran Tercatat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.20f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.40f))
                        ) {
                            Text(
                                text = "$completedCount Selesai",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        if (failedCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.40f))
                            ) {
                                Text(
                                    text = "$failedCount Gagal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // History Orders List
            if (historyOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = KubeTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada riwayat pengiriman selesai.",
                            fontSize = 14.sp,
                            color = KubeTextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list_delivery_history"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyOrders, key = { it.id }) { order ->
                        val meta = supabaseService.getOrderMeta(order.id)
                        HistoryOrderItemCard(
                            order = order,
                            meta = meta,
                            onClick = { selectedDetailOrder = order }
                        )
                    }
                }
            }
        }

        selectedDetailOrder?.let { detailOrder ->
            DeliveryHistoryDetailBottomSheet(
                order = detailOrder,
                meta = supabaseService.getOrderMeta(detailOrder.id),
                onDismiss = { selectedDetailOrder = null }
            )
        }
    }
}

@Composable
internal fun HistoryOrderItemCard(
    order: DeliveryOrder,
    meta: OrderPaymentMeta? = CourierSupabaseService.instance.getOrderMeta(order.id),
    onClick: () -> Unit = {}
) {
    val isDelivered = order.status == DeliveryStatus.DELIVERED
    val statusBg = if (isDelivered) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val statusText = if (isDelivered) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusLabel = if (isDelivered) "Terkirim (Selesai)" else "Gagal / Retur"

    val isCod = meta?.isCodOrder ?: order.isCod
    val paymentMethodLabel = when {
        isCod -> "COD Tunai"
        !meta?.paymentMethodName.isNullOrBlank() -> meta!!.paymentMethodName
        order.paymentMethod.isNotBlank() -> order.paymentMethod
        else -> "Non-Tunai"
    }
    val totalAmount = meta?.totalAmount?.takeIf { it > 0L } ?: order.totalAmount
    val commissionAmount = meta?.commissionAmount?.takeIf { it > 0L }
        ?: (order.defaultQuantity.coerceAtLeast(1) * 5000L)

    val recipientName = meta?.recipientName?.takeIf { it.isNotBlank() }
        ?: order.proofOfDelivery?.recipientName?.takeIf { it.isNotBlank() }
        ?: "-"
    val completedTime = meta?.deliveredAtFormatted?.takeIf { it.isNotBlank() }
        ?: order.deliveredTime?.takeIf { it.isNotBlank() }
        ?: order.inTransitTime?.takeIf { it.isNotBlank() }
        ?: order.assignedTime

    KubeGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("history_card_${order.id}"),
        shape = RoundedCornerShape(22.dp),
        isHighlighted = false,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // 1. Baris Header: Nama pelanggan/kafe, kode pesanan, dan badge status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KubeGlassIconBox(
                        icon = if (isDelivered) Icons.Filled.Store else Icons.Filled.HighlightOff,
                        contentDescription = null,
                        tint = if (isDelivered) KubeBlueBright else Color(0xFFC62828),
                        boxSize = 40.dp,
                        iconSize = 20.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.customerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "#${order.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A4C)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                KubeGlassPill(
                    text = statusLabel,
                    textColor = statusText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Baris Alamat Lengkap
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = order.address,
                    fontSize = 12.sp,
                    color = Color(0xFF1E3A4C),
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Kotak Rincian Barang Es (blok tersendiri, maxLines = 3, lineHeight = 18.sp)
            KubeGlassCard(
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.itemDescription,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Grid Ringkasan Keuangan & Pengantaran (2 Kolom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KubeGlassCard(
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(
                            text = paymentMethodLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A4C),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatHistoryCardRupiah(totalAmount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                KubeGlassCard(
                    shape = RoundedCornerShape(14.dp),
                    isHighlighted = true,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(
                            text = "Komisi Kurir",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF065F46),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "+ ${formatHistoryCardRupiah(commissionAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            // 5. Footer Penerima & Waktu Selesai
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Diterima: $recipientName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3A4C),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("history_recipient_${order.id}")
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF1E3A4C),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = completedTime,
                        fontSize = 11.sp,
                        color = Color(0xFF1E3A4C),
                        softWrap = false,
                        modifier = Modifier.testTag("history_time_${order.id}")
                    )
                }
            }
        }
    }
}

private fun formatHistoryCardRupiah(amount: Long): String {
    return "Rp ${"%,d".format(amount).replace(',', '.')}"
}
