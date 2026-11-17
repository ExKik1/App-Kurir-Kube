package com.example.features.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.LiquidDarkGlassCard
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.network.CourierSupabaseService
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
    val initialOrders = remember(repository) { repository.orders.value }
    val allOrders by supabaseService.streamMyDeliveries().collectAsState(initial = initialOrders)
    // Filter HANYA pesanan berstatus Selesai atau Gagal
    val historyOrders = remember(allOrders) {
        allOrders.filter {
            it.status == DeliveryStatus.DELIVERED || it.status == DeliveryStatus.FAILED
        }
    }

    val completedCount = historyOrders.count { it.status == DeliveryStatus.DELIVERED }
    val failedCount = historyOrders.count { it.status == DeliveryStatus.FAILED }

    LiquidGlassBackground(
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
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(3.dp, CircleShape, ambientColor = Color(0x1A0083B0))
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)), CircleShape),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.50f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_delivery_history")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = KubeTextDark
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
                        color = KubeTextMuted
                    )
                }
            }

            // Summary Metric Card
            LiquidDarkGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyOrders, key = { it.id }) { order ->
                        HistoryOrderItemCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryOrderItemCard(order: DeliveryOrder) {
    val isDelivered = order.status == DeliveryStatus.DELIVERED
    val statusBg = if (isDelivered) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val statusText = if (isDelivered) Color(0xFF2E7D32) else Color(0xFFC62828)
    val statusLabel = if (isDelivered) "Terkirim (Selesai)" else "Gagal / Retur"

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${order.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = 3.dp,
        fillAlphaTop = 0.55f,
        fillAlphaBottom = 0.28f,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.35f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Customer Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDelivered) Color(0xFFE0F2FE) else Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDelivered) Icons.Filled.Store else Icons.Filled.HighlightOff,
                            contentDescription = null,
                            tint = if (isDelivered) KubeBlueBright else Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = order.customerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Text(
                            text = "Pesanan #${order.id}",
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusText.copy(alpha = 0.30f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDelivered) Icons.Filled.CheckCircle else Icons.Filled.HighlightOff,
                            contentDescription = null,
                            tint = statusText,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${order.address}, ${order.subDistrict}",
                    fontSize = 12.sp,
                    color = KubeTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Item Description & Timestamp Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.itemDescription,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KubeTextDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = KubeTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = order.deliveredTime ?: order.inTransitTime ?: order.assignedTime,
                        fontSize = 11.sp,
                        color = KubeTextMuted
                    )
                }
            }
        }
    }
}
