package com.example.features.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.DarkObsidianGlassCard
import com.example.core.components.DbBodyText
import com.example.core.components.DbTitleText
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
import com.example.core.repository.DailyStatsState
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    repository: OrderRepository = OrderRepository.instance,
    onNavigateToDelivery: (filterStatus: DeliveryStatus?) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit = {}
) {
    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialCourier = remember(repository) { repository.courier.value }
    val initialOrders = remember(repository) { repository.orders.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }

    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val orders by supabaseService.streamMyDeliveries(courierProfileId).collectAsState(initial = initialOrders)
    // Single Source of Truth for all dashboard metrics: DailyStatsProvider
    val dailyStats = remember(orders) { supabaseService.calculateTodaySummary(orders) }

    val activeRunningOrders = remember(orders) { orders.filter { it.isAktif } }
    val primaryActiveOrder = activeRunningOrders.firstOrNull()
    val activePackageCount = remember(activeRunningOrders) {
        activeRunningOrders.sumOf { it.defaultQuantity.coerceAtLeast(1) }
    }
    val estimatedEarnings = remember(orders) {
        orders.filter { it.status != DeliveryStatus.FAILED }.sumOf { order ->
            supabaseService.getOrderMeta(order.id)?.commissionAmount?.takeIf { it > 0L }
                ?: (order.defaultQuantity.coerceAtLeast(1) * 5000L)
        }
    }

    val coroutineScope = rememberCoroutineScope()
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

    val scrollState = rememberScrollState()

    // Transparent host sitting directly over MainWrapper's KubeAtmosphericBackground
    Box(
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
            // Top Bar dengan akses cepat toggle On-Duty / Off-Duty langsung dari badge status
            TopCourierBar(
                name = courier.name,
                statusSubtitle = if (courier.isActiveDuty) "Selamat Bertugas" else "Sedang Istirahat (Off-Duty)",
                periodBadgeText = dailyStats.periodLabel, // 'Hari Ini'
                isOnDuty = courier.isActiveDuty,
                textColor = KubeTextDark,
                onAvatarClick = onNavigateToProfile,
                onToggleDutyClick = toggleDutyAction,
                onNotifClick = onNavigateToNotifications
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (!courier.isActiveDuty) {
                OffDutyWarningBanner(
                    onActivateOnDuty = toggleDutyAction,
                    isToggling = isTogglingDuty,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SEKSI 1 (Paling Atas - Prioritas Utama): Actionable priority alert card
            PriorityActionSection(
                orders = orders,
                dailyStats = dailyStats,
                onNavigateToDelivery = onNavigateToDelivery
            )

            Spacer(modifier = Modifier.height(22.dp))

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
                    KubeGlassPill(
                        text = dailyStats.periodLabel, // 'Hari Ini'
                        textColor = Color(0xFF0284C7),
                        modifier = Modifier.testTag("jobdesk_period_badge")
                    )
                }

                Text(
                    text = "See all",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0B4F6C),
                    modifier = Modifier
                        .clickable { onNavigateToDelivery(null) }
                        .testTag("jobdesk_see_all_button")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Jobdesk: Tugas Baru (kiri frosted opaque), Sedang Diantar (kanan translucent teal), Gagal/Retur
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
                    NewTasksFrostedCard(
                        count = dailyStats.newTasksOrders,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToDelivery(DeliveryStatus.NEW) }
                    )
                    InDeliveryTranslucentCard(
                        inDeliveryCount = dailyStats.inDeliveryOrders,
                        activeOrder = primaryActiveOrder,
                        activePackageCount = activePackageCount,
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

            Spacer(modifier = Modifier.height(22.dp))

            // Card Utama Performa Pengiriman: Dark Obsidian Glass Card
            PerformanceGlassCard(
                dailyStats = dailyStats,
                estimatedEarnings = estimatedEarnings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
        }
    }
}

/**
 * Kartu Kiri "Tugas Baru": KubeGlassCard(isHighlighted = false) + KubeGlassIconBox
 */
@Composable
private fun NewTasksFrostedCard(
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    KubeGlassCard(
        modifier = modifier
            .height(152.dp)
            .testTag("jobdesk_card_tugas_baru"),
        shape = RoundedCornerShape(24.dp),
        isHighlighted = false,
        contentPadding = PaddingValues(16.dp),
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
                KubeGlassIconBox(
                    icon = Icons.Filled.Inbox,
                    contentDescription = "Tugas Baru",
                    tint = Color(0xFF0284C7),
                    isHighlighted = false,
                    boxSize = 44.dp,
                    iconSize = 22.dp
                )

                Text(
                    text = "$count",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.testTag("count_tugas_baru")
                )
            }

            Column {
                DbTitleText(
                    text = "Tugas Baru",
                    color = Color(0xFF0F172A),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                DbBodyText(
                    text = "Menunggu diambil",
                    maxLines = 1,
                    color = Color(0xFF1E3A4C)
                )
            }
        }
    }
}

/**
 * Kartu Kanan "Sedang Diantar": KubeGlassCard(isHighlighted = true) + KubeGlassIconBox(isHighlighted = true)
 * Memancarkan kilau cyan transparan di bagian atas dan menampilkan angka counter warna putih,
 * lengkap dengan baris status live (jumlah paket • ETA • alamat tujuan).
 */
@Composable
private fun InDeliveryTranslucentCard(
    inDeliveryCount: Int,
    activeOrder: DeliveryOrder?,
    activePackageCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    KubeGlassCard(
        modifier = modifier
            .height(152.dp)
            .testTag("jobdesk_card_sedang_diantar"),
        shape = RoundedCornerShape(24.dp),
        isHighlighted = true,
        contentPadding = PaddingValues(16.dp),
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
                KubeGlassIconBox(
                    icon = Icons.Filled.LocalShipping,
                    contentDescription = "Sedang Diantar",
                    isHighlighted = true,
                    boxSize = 44.dp,
                    iconSize = 22.dp
                )

                Text(
                    text = "$inDeliveryCount",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.testTag("count_sedang_diantar")
                )
            }

            Column {
                Text(
                    text = "Sedang Diantar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (activeOrder != null) {
                    val shortDestination = activeOrder.subDistrict.takeIf { it.isNotBlank() && it != "-" }
                        ?: activeOrder.address
                    Text(
                        text = "$activePackageCount paket • Est. ${activeOrder.estimatedTimeMinutes} mnt",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0F7FA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = shortDestination,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Dalam perjalanan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE0F7FA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Belum ada rute aktif",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Kartu "Gagal / Retur": KubeGlassCard(isHighlighted = false) + KubeGlassIconBox + KubeGlassPill
 */
@Composable
private fun JobdeskReturnActionCard(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KubeGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .testTag("jobdesk_card_gagal_retur"),
        shape = RoundedCornerShape(22.dp),
        isHighlighted = false,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KubeGlassIconBox(
                    icon = Icons.Filled.AssignmentReturn,
                    contentDescription = "Gagal / Retur",
                    tint = if (count > 0) Color(0xFFD32F2F) else Color(0xFF0B3050),
                    isHighlighted = false,
                    boxSize = 44.dp,
                    iconSize = 22.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    DbTitleText(
                        text = "Gagal / Retur",
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    DbBodyText(
                        text = if (count > 0) "$count pesanan perlu tindak lanjut depo" else "Tidak ada pesanan bermasalah hari ini",
                        maxLines = 1,
                        color = if (count > 0) Color(0xFFC62828) else Color(0xFF1E3A4C)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KubeGlassPill(
                    text = "$count tugas",
                    textColor = if (count > 0) Color(0xFFB71C1C) else KubePrimaryText
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Buka retur",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Kartu Utama "Performa Pengiriman": DarkObsidianGlassCard
 * - Cincin progres melingkar di kiri dengan gradasi bercahaya Cyan (#22D3EE) ke Emerald (#34D399)
 * - Banner pil horizontal di tengah ("Estimasi Pendapatan: Rp ... • Efisiensi: ...%")
 * - 4 kotak metrik mini berwarna lembut (Selesai, Diantar, Menunggu, Retur) dengan angka tnum tebal
 */
@Composable
private fun PerformanceGlassCard(
    dailyStats: DailyStatsState,
    estimatedEarnings: Long,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = dailyStats.progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "circular_performance_progress"
    )
    val percentage = (animatedProgress * 100).toInt()
    val efficiencyPercent = if (dailyStats.totalOrders > 0) {
        ((dailyStats.completedOrders.toFloat() / dailyStats.totalOrders.toFloat()) * 100f).toInt().coerceIn(0, 100)
    } else {
        0
    }

    DarkObsidianGlassCard(
        modifier = modifier.testTag("performance_card"),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34D399))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Performa Pengiriman",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Rekapitulasi Historis • ${dailyStats.periodLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF67E8F9)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22D3EE).copy(alpha = 0.16f))
                        .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Grafik Performa",
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Center Display: Cyan-to-Emerald Glowing Circular Ring & Headline Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(106.dp)
                ) {
                    Canvas(modifier = Modifier.size(96.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Track Circle
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )

                        // Glowing Cyan (#22D3EE) -> Emerald (#34D399) Progress Arc
                        val sweepAngle = animatedProgress * 360f
                        if (sweepAngle > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF22D3EE), // Cyan
                                        Color(0xFF2DD4BF), // Teal
                                        Color(0xFF34D399), // Emerald
                                        Color(0xFF22D3EE)
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }

                    // Inside Ring Metric
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percentage%",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = "Tercapai",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${dailyStats.completedOrders}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF34D399),
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = "/${dailyStats.totalOrders}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f),
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selesai",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${(dailyStats.totalOrders - dailyStats.completedOrders).coerceAtLeast(0)} pesanan tersisa hari ini",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner Pil Horizontal di Tengah: Estimasi Pendapatan & Efisiensi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("performance_summary_pill"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Estimasi Pendapatan: ${formatDashboardRupiah(estimatedEarnings)} • Efisiensi: $efficiencyPercent%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.94f),
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Kotak Metrik Mini (Hijau Selesai, Kuning/Amber Diantar, Oranye Menunggu, Slate Retur)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ObsidianMetricPill(
                    number = "${dailyStats.completedOrders}",
                    label = "Selesai",
                    accentColor = Color(0xFF34D399),
                    bgColor = Color(0xFF10B981).copy(alpha = 0.16f),
                    borderColor = Color(0xFF34D399).copy(alpha = 0.40f),
                    modifier = Modifier.weight(1f)
                )
                ObsidianMetricPill(
                    number = "${dailyStats.inDeliveryOrders}",
                    label = "Diantar",
                    accentColor = Color(0xFFFBBF24),
                    bgColor = Color(0xFFF59E0B).copy(alpha = 0.16f),
                    borderColor = Color(0xFFFBBF24).copy(alpha = 0.40f),
                    modifier = Modifier.weight(1f)
                )
                ObsidianMetricPill(
                    number = "${dailyStats.newTasksOrders}",
                    label = "Menunggu",
                    accentColor = Color(0xFFFB923C),
                    bgColor = Color(0xFFF97316).copy(alpha = 0.16f),
                    borderColor = Color(0xFFFB923C).copy(alpha = 0.40f),
                    modifier = Modifier.weight(1f)
                )
                ObsidianMetricPill(
                    number = "${dailyStats.failedReturnOrders}",
                    label = "Retur",
                    accentColor = if (dailyStats.failedReturnOrders > 0) Color(0xFFFCA5A5) else Color(0xFFCBD5E1),
                    bgColor = Color(0xFF64748B).copy(alpha = 0.18f),
                    borderColor = Color(0xFF94A3B8).copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ObsidianMetricPill(
    number: String,
    label: String,
    accentColor: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

/**
 * SEKSI 1 (Paling Atas - Prioritas Utama):
 * Kartu peringatan actionable yang bisa diketuk untuk menindaklanjuti
 * kendala pengiriman (retur), tenggat waktu mendesak, atau tugas menunggu konfirmasi.
 */
@Composable
private fun PriorityActionSection(
    orders: List<DeliveryOrder>,
    dailyStats: DailyStatsState,
    onNavigateToDelivery: (DeliveryStatus?) -> Unit
) {
    val urgentOrder = remember(orders) {
        orders.firstOrNull {
            !it.isRiwayat && (it.priority.contains("Tinggi", ignoreCase = true) ||
                it.priority.contains("Mendesak", ignoreCase = true))
        }
    }
    val hasIssue = dailyStats.failedReturnOrders > 0
    val hasPendingConfirm = dailyStats.newTasksOrders > 0

    val targetFilter: DeliveryStatus?
    val badgeText: String
    val headline: String
    val subtitle: String
    val accentColor: Color
    val leadingIcon: ImageVector

    when {
        hasIssue -> {
            targetFilter = DeliveryStatus.FAILED
            badgeText = "KENDALA"
            headline = "${dailyStats.failedReturnOrders} Pengiriman Gagal / Retur"
            subtitle = "Segera konfirmasi pengembalian stok ke depo Kube"
            accentColor = Color(0xFFDC2626)
            leadingIcon = Icons.Filled.PriorityHigh
        }
        urgentOrder != null -> {
            targetFilter = urgentOrder.status
            badgeText = "MENDESAK"
            headline = "${urgentOrder.customerName} (${urgentOrder.id})"
            subtitle = "${urgentOrder.itemDescription} • Est. ${urgentOrder.estimatedTimeMinutes} mnt • Ketuk untuk proses"
            accentColor = Color(0xFFD97706)
            leadingIcon = Icons.Filled.Schedule
        }
        hasPendingConfirm -> {
            targetFilter = DeliveryStatus.NEW
            badgeText = "MENUNGGU"
            headline = "${dailyStats.newTasksOrders} Tugas Baru Menunggu Konfirmasi"
            subtitle = "Ketuk untuk melihat antrean pengambilan es di gudang"
            accentColor = Color(0xFF0284C7)
            leadingIcon = Icons.Filled.NotificationsActive
        }
        else -> {
            targetFilter = DeliveryStatus.IN_TRANSIT
            badgeText = "AMAN"
            headline = "Semua Prioritas Terkendali"
            subtitle = "Tidak ada kendala mendesak. Lanjutkan pengantaran aktif Anda."
            accentColor = Color(0xFF059669)
            leadingIcon = Icons.Filled.CheckCircle
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("section_prioritas_utama")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Prioritas Utama",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KubePrimaryText
            )
            KubeGlassPill(
                text = badgeText,
                textColor = accentColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        KubeGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("priority_action_card"),
            shape = RoundedCornerShape(22.dp),
            isHighlighted = hasIssue,
            contentPadding = PaddingValues(16.dp),
            onClick = { onNavigateToDelivery(targetFilter) }
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
                        icon = leadingIcon,
                        contentDescription = "Prioritas Utama",
                        tint = accentColor,
                        isHighlighted = false,
                        boxSize = 44.dp,
                        iconSize = 22.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        DbTitleText(
                            text = headline,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        DbBodyText(
                            text = subtitle,
                            maxLines = 2,
                            color = Color(0xFF1E3A4C)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Buka Prioritas",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatDashboardRupiah(amount: Long): String {
    return "Rp ${"%,d".format(amount).replace(',', '.')}"
}
