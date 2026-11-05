package com.example.features.dashboard

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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.LiquidDarkGlassCard
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.core.components.TopCourierBar
import com.example.core.model.DeliveryStatus
import com.example.core.repository.DailyStatsProvider
import com.example.core.repository.DailyStatsState
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark

@Composable
fun DashboardScreen(
    repository: OrderRepository = OrderRepository.instance,
    onNavigateToDelivery: (filterStatus: DeliveryStatus?) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val courier by repository.courier.collectAsState()
    // Single Source of Truth for all dashboard metrics: DailyStatsProvider
    val dailyStats by repository.dailyStats.collectAsState()

    var showBalanceDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Dompet Kurir Kube", fontWeight = FontWeight.Bold, color = KubeTextDark) },
            text = {
                Column {
                    Text("Saldo Siap Cair:", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Rp ${"%,d".format(courier.balance).replace(',', '.')}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeBlueBright
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Pencairan otomatis setiap hari Jumat ke rekening BCA terverifikasi.",
                        fontSize = 13.sp,
                        color = Color(0xFF4A6B82)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBalanceDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold, color = KubeBlueBright)
                }
            }
        )
    }

    // Liquid Glass Scaffold Background (#F5F7FA with ambient liquid glow)
    LiquidGlassBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            // Top Bar with Maryono Avatar & Explicit 'Hari Ini' Period Tag
            TopCourierBar(
                name = courier.name,
                statusSubtitle = courier.statusText,
                periodBadgeText = dailyStats.periodLabel, // 'Hari Ini'
                isOnDuty = courier.isActiveDuty,
                textColor = KubeTextDark,
                onAvatarClick = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Check Balance Frosted Liquid Glass Banner
            CheckBalanceGlassBanner(
                onClick = { showBalanceDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Jobdesk Header with explicit 'Hari Ini' Period Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Jobdesk",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    // Explicit Daily Period Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00B4DB).copy(alpha = 0.12f))
                            .border(
                                BorderStroke(1.dp, Color(0xFF00B4DB).copy(alpha = 0.40f)),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .testTag("jobdesk_period_badge")
                    ) {
                        Text(
                            text = dailyStats.periodLabel, // 'Hari Ini'
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeBlueBright
                        )
                    }
                }

                Text(
                    text = "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KubeBlueBright,
                    modifier = Modifier
                        .clickable { onNavigateToDelivery(null) }
                        .testTag("jobdesk_see_all_button")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Jobdesk: KHUSUS Pesanan yang masih butuh tindakan kurir
            // (Tugas Baru, Sedang Diantar, Gagal/Retur)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Baris 1: Tugas Baru & Sedang Diantar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JobdeskActionCard(
                        title = "Tugas Baru",
                        count = dailyStats.newTasksOrders,
                        subtitle = "Menunggu diambil",
                        icon = Icons.Filled.Inbox,
                        iconBackground = Color(0xFFE1F5FE),
                        iconTint = Color(0xFF0277BD),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToDelivery(DeliveryStatus.NEW) }
                    )
                    JobdeskActionCard(
                        title = "Sedang Diantar",
                        count = dailyStats.inDeliveryOrders,
                        subtitle = "Dalam perjalanan",
                        icon = Icons.Filled.LocalShipping,
                        iconBackground = Color(0xFFE0F7FA),
                        iconTint = Color(0xFF00838F),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToDelivery(DeliveryStatus.IN_TRANSIT) }
                    )
                }

                // Baris 2: Gagal / Retur (Tindakan retur / konfirmasi depo)
                JobdeskReturnActionCard(
                    count = dailyStats.failedReturnOrders,
                    onClick = { onNavigateToDelivery(DeliveryStatus.FAILED) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card Performa Pengiriman: Rekapitulasi Historis Harian
            // Penjumlahan akurat secara matematis: Total = Selesai + Diantar + Menunggu + Retur
            PerformanceGlassCard(
                dailyStats = dailyStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun CheckBalanceGlassBanner(onClick: () -> Unit) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("check_balance_card"),
        shape = RoundedCornerShape(26.dp),
        elevation = 6.dp,
        fillAlphaTop = 0.50f,
        fillAlphaBottom = 0.25f,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.35f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Wallet Icon in white frosted circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.75f))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = "Wallet",
                        tint = Color(0xFF0F3657),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Check Balance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
            }

            Text(
                text = ">>>",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KubeBlueBright,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

/**
 * Card Jobdesk untuk tindakan kurir: Tugas Baru dan Sedang Diantar
 */
@Composable
private fun JobdeskActionCard(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier
            .height(134.dp)
            .testTag("jobdesk_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp,
        fillAlphaTop = 0.55f,
        fillAlphaBottom = 0.28f,
        tintColor = Color.White,
        borderAlphaTop = 0.92f,
        borderAlphaBottom = 0.40f,
        borderWidth = 1.2.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Frosted Icon container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Distinctive Count Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.70f))
                        .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0B3050)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF5E7E94)
                )
            }
        }
    }
}

/**
 * Card Jobdesk untuk Gagal / Retur (tindakan kurir yang membutuhkan follow up depo)
 */
@Composable
private fun JobdeskReturnActionCard(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .testTag("jobdesk_card_gagal_retur"),
        shape = RoundedCornerShape(22.dp),
        elevation = 5.dp,
        fillAlphaTop = 0.50f,
        fillAlphaBottom = 0.25f,
        tintColor = Color.White,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.38f,
        borderWidth = 1.2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Frosted return icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (count > 0) Color(0xFFFFEBEE) else Color.White.copy(alpha = 0.85f)
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AssignmentReturn,
                        contentDescription = "Gagal / Retur",
                        tint = if (count > 0) Color(0xFFD32F2F) else Color(0xFF0B3050),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Gagal / Retur",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (count > 0) "$count pesanan perlu tindak lanjut depo" else "Tidak ada pesanan bermasalah hari ini",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (count > 0) Color(0xFFC62828) else Color(0xFF5E7E94)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Count tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (count > 0) Color(0xFFFFCDD2) else Color.White.copy(alpha = 0.70f)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (count > 0) Color(0xFFEF5350) else Color.White
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$count tugas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count > 0) Color(0xFFB71C1C) else Color(0xFF0B3050)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Buka retur",
                    tint = Color(0xFF5E7E94),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Card Performa Pengiriman: Rekapitulasi Historis Harian
 * Single Source of Truth dari DailyStatsState.
 * Penjumlahan akurat secara matematis:
 * Total = Selesai + Diantar + Menunggu + Retur (12 + 5 + 3 + 0 = 20)
 */
@Composable
private fun PerformanceGlassCard(
    dailyStats: DailyStatsState,
    modifier: Modifier = Modifier
) {
    LiquidDarkGlassCard(
        modifier = modifier.testTag("performance_card"),
        shape = RoundedCornerShape(26.dp),
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Performa Pengiriman",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Rekapitulasi Historis • ${dailyStats.periodLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = KubeCyan
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Grafik Performa",
                    tint = KubeCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Headline: e.g. "12/20 Selesai" with Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${dailyStats.completedOrders}/${dailyStats.totalOrders} Selesai",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                // Percentage Badge
                val percentage = (dailyStats.progressFraction * 100).toInt()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(KubeCyan.copy(alpha = 0.20f))
                        .border(BorderStroke(1.dp, KubeCyan.copy(alpha = 0.50f)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percentage% Tercapai",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { dailyStats.progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = KubeCyan,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Penjelasan Matematis Akurat (Formula Check)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(BorderStroke(0.6.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Total ${dailyStats.totalOrders} pesanan = ${dailyStats.completedOrders} Selesai + ${dailyStats.inDeliveryOrders} Diantar + ${dailyStats.newTasksOrders} Menunggu${if (dailyStats.failedReturnOrders > 0) " + ${dailyStats.failedReturnOrders} Retur" else ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Sub-pills Rekapitulasi Sinkron dengan Jobdesk:
            // Selesai (12) + Diantar (5) + Menunggu (3) + Retur (0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerformanceMetricGlassPill(
                    number = "${dailyStats.completedOrders}",
                    label = "Selesai",
                    accentColor = KubeCyan,
                    modifier = Modifier.weight(1f)
                )
                PerformanceMetricGlassPill(
                    number = "${dailyStats.inDeliveryOrders}",
                    label = "Diantar",
                    accentColor = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                )
                PerformanceMetricGlassPill(
                    number = "${dailyStats.newTasksOrders}",
                    label = "Menunggu",
                    accentColor = Color(0xFFFFB74D),
                    modifier = Modifier.weight(1f)
                )
                PerformanceMetricGlassPill(
                    number = "${dailyStats.failedReturnOrders}",
                    label = "Retur",
                    accentColor = if (dailyStats.failedReturnOrders > 0) Color(0xFFE57373) else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetricGlassPill(
    number: String,
    label: String,
    accentColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}
