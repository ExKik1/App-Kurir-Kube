package com.example.features.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.core.network.CourierSupabaseService
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
    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialCourier = remember(repository) { repository.courier.value }
    val initialOrders = remember(repository) { repository.orders.value }
    val initialWallet = remember(repository) { repository.walletBalance.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }

    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val orders by supabaseService.streamMyDeliveries(courierProfileId).collectAsState(initial = initialOrders)
    val walletBalance by supabaseService.streamWalletSummary(courierProfileId).collectAsState(initial = initialWallet)
    val notifications by supabaseService.streamNotifications(courierProfileId).collectAsState(initial = emptyList())
    // Single Source of Truth for all dashboard metrics: DailyStatsProvider
    val dailyStats = remember(orders) { supabaseService.calculateTodaySummary(orders) }

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
                        "Rp ${"%,d".format(walletBalance.availableBalance).replace(',', '.')}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeBlueBright
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Pencairan ke rekening ${courier.bankName.ifBlank { "bank" }} terverifikasi (${notifications.size} notifikasi aktif).",
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

            // SEKSI 1 (Paling Atas - Prioritas Utama): Actionable priority alert card
            PriorityActionSection(
                orders = orders,
                dailyStats = dailyStats,
                onNavigateToDelivery = onNavigateToDelivery
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SEKSI 2: Interactive Glassmorphism Balance Card with Hidden/Visible Toggle & Glowing Halo
            CheckBalanceGlassBanner(
                balance = walletBalance.availableBalance,
                onViewDetails = { showBalanceDialog = true }
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

/**
 * ATURAN 2: Rombak Fitur 'Check Balance' Menjadi Interaktif
 * - Card saldo bergaya Glassmorphism premium
 * - Nilai tersembunyi secara default: "Rp *******"
 * - Ikon mata (Icons.Filled.Visibility / VisibilityOff)
 * - Di-klik memunculkan saldo asli dengan animasi fade / slide halus
 * - Efek glow (bayangan cahaya biru tipis) di sekitar card
 */
@Composable
private fun CheckBalanceGlassBanner(
    balance: Long,
    onViewDetails: () -> Unit
) {
    var isBalanceVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Efek Glow (bayangan cahaya biru lembut di belakang card)
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF00B4DB).copy(alpha = 0.22f))
                .border(
                    BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.40f)),
                    RoundedCornerShape(26.dp)
                )
        )

        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isBalanceVisible = !isBalanceVisible }
                .testTag("check_balance_card"),
            shape = RoundedCornerShape(26.dp),
            elevation = 6.dp,
            fillAlphaTop = 0.60f,
            fillAlphaBottom = 0.32f,
            borderAlphaTop = 0.95f,
            borderAlphaBottom = 0.45f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
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
                        // Frosted wallet icon in glass circle
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Saldo Siap Cair",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF5E7E94)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (isBalanceVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isBalanceVisible) "Sembunyikan Saldo" else "Tampilkan Saldo",
                                    tint = KubeBlueBright,
                                    modifier = Modifier
                                        .size(17.dp)
                                        .clickable { isBalanceVisible = !isBalanceVisible }
                                        .testTag("toggle_balance_visibility")
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            // AnimatedSwitcher equivalent: smooth crossfade between Rp ******* and real balance
                            AnimatedContent(
                                targetState = isBalanceVisible,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "balance_visibility_switcher"
                            ) { visible ->
                                if (visible) {
                                    Text(
                                        text = "Rp ${"%,d".format(balance).replace(',', '.')}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F3657),
                                        modifier = Modifier.testTag("visible_balance_text")
                                    )
                                } else {
                                    Text(
                                        text = "Rp *******",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F3657),
                                        modifier = Modifier.testTag("hidden_balance_mask")
                                    )
                                }
                            }
                        }
                    }

                    // Action pill to open details
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF00B4DB).copy(alpha = 0.15f))
                            .border(
                                BorderStroke(1.dp, Color(0xFF00B4DB).copy(alpha = 0.40f)),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(onClick = onViewDetails)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("btn_view_balance_details")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rincian",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeBlueBright
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = KubeBlueBright,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
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
/**
 * ATURAN 3: Upgrade UI 'Performa Pengiriman'
 * - Latar belakang gelap semi-transparan (GlassContainer warna biru navy/midnight blue dengan blur & border specular)
 * - Animasi Circular Progress Indicator berdesain modern (rounded caps, gradasi warna cyan ke biru)
 * - Tipografi angka (12 Selesai, 5 Diantar, 3 Menunggu) menonjol (FontWeight.Bold)
 * - Aksen warna hijau terang (#00E676) untuk 'Selesai' dan kuning terang (#FFD600) untuk 'Diantar' agar UI terasa lebih 'hidup' dan informatif.
 */
@Composable
private fun PerformanceGlassCard(
    dailyStats: DailyStatsState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = dailyStats.progressFraction.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 1000,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "circular_performance_progress"
    )

    // Midnight Blue / Deep Navy Glass Card with Specular Border & Ambient Cyan Sheen
    Surface(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x6600B4DB),
                spotColor = Color(0x4400E5FF),
                clip = false
            )
            .clip(RoundedCornerShape(26.dp))
            .border(
                BorderStroke(
                    1.2.dp,
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.25f),
                            Color(0xFF0083B0).copy(alpha = 0.45f)
                        )
                    )
                ),
                RoundedCornerShape(26.dp)
            )
            .testTag("performance_card"),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF071B2F).copy(alpha = 0.94f), // Deep Midnight Navy
                            Color(0xFF030D18).copy(alpha = 0.97f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                    .background(Color(0xFF00E676)) // Bright Green live indicator
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
                            color = Color(0xFF80DEEA)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00B4DB).copy(alpha = 0.18f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Grafik Performa",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Center Display: Modern Circular Progress Gauge & Headline Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Modern Animated Circular Progress Gauge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(105.dp)
                    ) {
                        Canvas(modifier = Modifier.size(96.dp)) {
                            val strokeWidth = 10.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            // Track Circle (Dark Navy Translucent)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = radius,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth
                                )
                            )

                            // Glowing Progress Arc with Rounded Caps & Cyan-to-Blue Gradient
                            val sweepAngle = animatedProgress * 360f
                            if (sweepAngle > 0f) {
                                drawArc(
                                    brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF), // Electric Cyan
                                            Color(0xFF00B4DB),
                                            Color(0xFF0288D1),
                                            Color(0xFF00E5FF)
                                        )
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = Offset(center.x - radius, center.y - radius),
                                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                        }

                        // Inside Ring Metric
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val percentage = (animatedProgress * 100).toInt()
                            Text(
                                text = "$percentage%",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Tercapai",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Primary Delivery Progress Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${dailyStats.completedOrders}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676) // Bright Vibrant Green
                            )
                            Text(
                                text = "/${dailyStats.totalOrders}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Selesai",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${dailyStats.totalOrders - dailyStats.completedOrders} pesanan tersisa hari ini",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB0BEC5)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Formula Check Badge (Akurasi Matematis)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF021727).copy(alpha = 0.70f))
                        .border(
                            BorderStroke(0.8.dp, Color(0xFF00E5FF).copy(alpha = 0.25f)),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "Total ${dailyStats.totalOrders} = ${dailyStats.completedOrders} Selesai + ${dailyStats.inDeliveryOrders} Diantar + ${dailyStats.newTasksOrders} Menunggu${if (dailyStats.failedReturnOrders > 0) " + ${dailyStats.failedReturnOrders} Retur" else ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.90f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4 Futuristic Sub-pills with vibrant colored typography:
                // Selesai (Hijau Terang #00E676), Diantar (Kuning Terang #FFD600), Menunggu (Orange #FF9800), Retur (Merah muda)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FuturisticMetricPill(
                        number = "${dailyStats.completedOrders}",
                        label = "Selesai",
                        accentColor = Color(0xFF00E676), // Bright Green
                        bgColor = Color(0xFF00E676).copy(alpha = 0.12f),
                        borderColor = Color(0xFF00E676).copy(alpha = 0.40f),
                        modifier = Modifier.weight(1f)
                    )
                    FuturisticMetricPill(
                        number = "${dailyStats.inDeliveryOrders}",
                        label = "Diantar",
                        accentColor = Color(0xFFFFD600), // Vibrant Yellow
                        bgColor = Color(0xFFFFD600).copy(alpha = 0.12f),
                        borderColor = Color(0xFFFFD600).copy(alpha = 0.40f),
                        modifier = Modifier.weight(1f)
                    )
                    FuturisticMetricPill(
                        number = "${dailyStats.newTasksOrders}",
                        label = "Menunggu",
                        accentColor = Color(0xFFFF9100), // Vibrant Orange
                        bgColor = Color(0xFFFF9100).copy(alpha = 0.12f),
                        borderColor = Color(0xFFFF9100).copy(alpha = 0.35f),
                        modifier = Modifier.weight(1f)
                    )
                    FuturisticMetricPill(
                        number = "${dailyStats.failedReturnOrders}",
                        label = "Retur",
                        accentColor = if (dailyStats.failedReturnOrders > 0) Color(0xFFFF5252) else Color.White.copy(alpha = 0.55f),
                        bgColor = if (dailyStats.failedReturnOrders > 0) Color(0xFFFF5252).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                        borderColor = if (dailyStats.failedReturnOrders > 0) Color(0xFFFF5252).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FuturisticMetricPill(
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
            .border(BorderStroke(1.2.dp, borderColor), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
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
    orders: List<com.example.core.model.DeliveryOrder>,
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
    val badgeBg: Color
    val leadingIcon: ImageVector

    when {
        hasIssue -> {
            targetFilter = DeliveryStatus.FAILED
            badgeText = "KENDALA"
            headline = "${dailyStats.failedReturnOrders} Pengiriman Gagal / Retur"
            subtitle = "Segera konfirmasi pengembalian stok ke depo Kube"
            accentColor = Color(0xFFDC2626)
            badgeBg = Color(0xFFFEE2E2)
            leadingIcon = Icons.Filled.PriorityHigh
        }
        urgentOrder != null -> {
            targetFilter = urgentOrder.status
            badgeText = "MENDESAK"
            headline = "${urgentOrder.customerName} (${urgentOrder.id})"
            subtitle = "${urgentOrder.itemDescription} • Est. ${urgentOrder.estimatedTimeMinutes} mnt • Ketuk untuk proses"
            accentColor = Color(0xFFD97706)
            badgeBg = Color(0xFFFEF3C7)
            leadingIcon = Icons.Filled.Schedule
        }
        hasPendingConfirm -> {
            targetFilter = DeliveryStatus.NEW
            badgeText = "MENUNGGU"
            headline = "${dailyStats.newTasksOrders} Tugas Baru Menunggu Konfirmasi"
            subtitle = "Ketuk untuk melihat antrean pengambilan es di gudang"
            accentColor = Color(0xFF0284C7)
            badgeBg = Color(0xFFE0F2FE)
            leadingIcon = Icons.Filled.NotificationsActive
        }
        else -> {
            targetFilter = DeliveryStatus.IN_TRANSIT
            badgeText = "AMAN"
            headline = "Semua Prioritas Terkendali"
            subtitle = "Tidak ada kendala mendesak. Lanjutkan pengantaran aktif Anda."
            accentColor = Color(0xFF059669)
            badgeBg = Color(0xFFD1FAE5)
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
                color = KubeTextDark
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("priority_action_card"),
            shape = RoundedCornerShape(22.dp),
            elevation = 6.dp,
            fillAlphaTop = 0.65f,
            fillAlphaBottom = 0.35f,
            borderAlphaTop = 0.95f,
            borderAlphaBottom = 0.45f,
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(badgeBg)
                            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = "Prioritas Utama",
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = headline,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF5A738E),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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

