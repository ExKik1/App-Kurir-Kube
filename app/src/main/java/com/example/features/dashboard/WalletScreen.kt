package com.example.features.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.LiquidDarkGlassCard
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.core.components.TopCourierBar
import com.example.core.model.CommissionHistoryItem
import com.example.core.model.WithdrawalHistoryItem
import com.example.core.model.WithdrawalStatus
import com.example.core.model.maskAccountNumber
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

@Composable
fun WalletScreen(
    repository: OrderRepository = OrderRepository.instance,
    onNavigateToProfile: () -> Unit = {}
) {
    val courier by repository.courier.collectAsState()
    val walletBalance by repository.walletBalance.collectAsState()
    val commissionList by repository.commissionHistory.collectAsState()
    val withdrawalList by repository.withdrawalHistory.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Riwayat Komisi, 1: Riwayat Pencairan
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("wallet_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 120.dp)
            ) {
                // Top Courier Bar
                TopCourierBar(
                    name = courier.name,
                    statusSubtitle = "Dompet & Keuangan Kurir",
                    isOnDuty = courier.isActiveDuty,
                    textColor = KubeTextDark,
                    onAvatarClick = onNavigateToProfile
                )

                // Header Title
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        text = "Dompet & Pendapatan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Transparansi saldo, komisi pengiriman, dan riwayat penarikan",
                        fontSize = 13.sp,
                        color = KubeTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==========================================
                // RULE 1: PEMISAHAN SALDO (3 CARDS HORIZONTAL/GRID)
                // Hapus 'Total Saldo Siap Cair' tunggal.
                // 3 Card: Saldo Tertunda (kuning/abu-abu), Saldo Tersedia (hijau), Saldo Dicairkan (biru)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Saldo Tertunda (Kuning / Abu-abu)
                    BalanceMetricCard(
                        title = "Saldo Tertunda",
                        amountText = formatRupiah(walletBalance.pendingBalance),
                        statusHint = "Menunggu verifikasi",
                        icon = Icons.Filled.AccessTime,
                        accentColor = Color(0xFFD97706),
                        containerColor = Color(0xFFFFFBEB),
                        borderColor = Color(0xFFFDE68A),
                        tag = "card_saldo_tertunda",
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Saldo Tersedia (Hijau)
                    BalanceMetricCard(
                        title = "Saldo Tersedia",
                        amountText = formatRupiah(walletBalance.availableBalance),
                        statusHint = "Siap dicairkan",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = Color(0xFF059669),
                        containerColor = Color(0xFFECFDF5),
                        borderColor = Color(0xFFA7F3D0),
                        tag = "card_saldo_tersedia",
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: Saldo Dicairkan (Biru)
                    BalanceMetricCard(
                        title = "Saldo Dicairkan",
                        amountText = formatRupiah(walletBalance.withdrawnBalance),
                        statusHint = "Total ditarik",
                        icon = Icons.Filled.Payment,
                        accentColor = Color(0xFF0284C7),
                        containerColor = Color(0xFFF0F9FF),
                        borderColor = Color(0xFFBAE6FD),
                        tag = "card_saldo_dicairkan",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Tarik Saldo Tersedia
                LiquidDarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("action_withdraw_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dana Siap Ditarik",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.80f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatRupiah(walletBalance.availableBalance),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeCyan
                            )
                        }

                        Button(
                            onClick = { showWithdrawDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_tarik_dana")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowOutward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tarik Dana",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // RULE 4: KEAMANAN REKENING (MASKING)
                // - String masking: 8830192831 -> **** **** 2831
                // - Tombol 'Edit Rekening' memunculkan dialog 'Perubahan rekening harus melalui verifikasi Admin'
                // ==========================================
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("card_rekening_bca"),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 4.dp,
                    fillAlphaTop = 0.55f,
                    fillAlphaBottom = 0.28f,
                    borderAlphaTop = 0.95f,
                    borderAlphaBottom = 0.35f
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bank Logo Box
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE3F2FD)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = "Bank BCA",
                                    tint = Color(0xFF0D47A1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "BCA • ${maskAccountNumber(courier.bankAccountNumber)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KubeTextDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "a.n. ${courier.bankAccountHolder}",
                                    fontSize = 12.sp,
                                    color = KubeTextMuted
                                )
                            }

                            // Verified badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Utama",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Edit Rekening Locked Button
                        OutlinedButton(
                            onClick = { showEditAccountDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF0096C7).copy(alpha = 0.40f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.60f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("btn_edit_rekening")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Rekening",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(Terkunci)",
                                fontSize = 11.sp,
                                color = KubeTextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // RULE 2 & RULE 3: TAB SELECTOR
                // Tab 1: Riwayat Komisi (Detail jenis pendapatan)
                // Tab 2: Riwayat Pencairan (Status Menunggu, Diproses, Berhasil, Ditolak + Alasan)
                // ==========================================
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.70f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WalletTabPill(
                            title = "Riwayat Komisi",
                            count = commissionList.size,
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            tag = "tab_riwayat_komisi",
                            modifier = Modifier.weight(1f)
                        )
                        WalletTabPill(
                            title = "Riwayat Pencairan",
                            count = withdrawalList.size,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            tag = "tab_riwayat_pencairan",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                if (selectedTab == 0) {
                    // ==========================================
                    // RULE 2: DETAIL RIWAYAT KOMISI
                    // Perjelas jenis pendapatan:
                    // 'Komisi 8 balok es - Rp45.000', 'Tip Pelanggan - Rp20.000', dll.
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag("list_riwayat_komisi"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        commissionList.forEach { commission ->
                            CommissionHistoryCard(item = commission)
                        }
                    }
                } else {
                    // ==========================================
                    // RULE 3: STATUS PENCAIRAN (WITHDRAWAL)
                    // Badge: Menunggu (Kuning), Diproses (Biru), Berhasil (Hijau), Ditolak (Merah + alasan)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .testTag("list_riwayat_pencairan"),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        withdrawalList.forEach { withdrawal ->
                            WithdrawalHistoryCard(item = withdrawal)
                        }
                    }
                }
            }
        }

        // ==========================================
        // DIALOG: PERUBAHAN REKENING HARUS MELALUI VERIFIKASI ADMIN (RULE 4)
        // ==========================================
        if (showEditAccountDialog) {
            AlertDialog(
                onDismissRequest = { showEditAccountDialog = false },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Terkunci",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Keamanan Rekening",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Perubahan rekening harus melalui verifikasi Admin",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }

                        Text(
                            text = "Demi melindungi dana dan integritas akun kurir, rekening bank yang telah didaftarkan tidak dapat diubah secara mandiri. Silakan hubungi admin operasional Kube dengan melampirkan foto buku tabungan baru untuk verifikasi identitas.",
                            fontSize = 13.sp,
                            color = KubeTextMuted,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "Rekening aktif saat ini: BCA • ${maskAccountNumber(courier.bankAccountNumber)} (a.n. ${courier.bankAccountHolder})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = KubeTextDark
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showEditAccountDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_close_edit_dialog")
                    ) {
                        Text("Mengerti", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // ==========================================
        // DIALOG / BOTTOM SHEET: TARIK SALDO DANA (RULE 2)
        // ==========================================
        if (showWithdrawDialog) {
            WithdrawalModalBottomSheet(
                availableBalance = walletBalance.availableBalance,
                bankAccountMasked = maskAccountNumber(courier.bankAccountNumber),
                accountHolder = courier.bankAccountHolder,
                onDismiss = { showWithdrawDialog = false },
                onConfirmWithdraw = { amount ->
                    val result = repository.requestWithdrawal(amount)
                    if (result.isSuccess) {
                        showWithdrawDialog = false
                        selectedTab = 1 // Switch to Riwayat Pencairan to show new status
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "Pengajuan penarikan dana berhasil dikirim. Admin akan memproses transfer Anda segera."
                            )
                        }
                    }
                    result
                }
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
        )
    }
}

/**
 * 3-Column horizontal balance card for Saldo Tertunda, Saldo Tersedia, and Saldo Dicairkan.
 */
@Composable
private fun BalanceMetricCard(
    title: String,
    amountText: String,
    statusHint: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    borderColor: Color,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(18.dp),
        color = containerColor.copy(alpha = 0.85f),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = amountText,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = KubeTextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = statusHint,
                fontSize = 9.sp,
                color = KubeTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Tab pill selector button with active indicator styling.
 */
@Composable
private fun WalletTabPill(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) KubeBlueBright else Color.Transparent
    val contentColor = if (isSelected) Color.White else KubeTextDark
    val countBgColor = if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.08f)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(countBgColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Card for Rule 2: Detail Riwayat Komisi
 * Clarified items: 'Komisi 8 balok es - Rp45.000', 'Tip Pelanggan - Rp20.000'
 */
@Composable
private fun CommissionHistoryCard(item: CommissionHistoryItem) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("commission_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = 3.dp,
        fillAlphaTop = 0.50f,
        fillAlphaBottom = 0.25f,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.35f
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isTip) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isTip) Icons.Filled.ThumbUp else Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = if (item.isTip) Color(0xFF059669) else Color(0xFF0284C7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 11.sp,
                        color = KubeTextMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
            ) {
                Text(
                    text = "+ ${formatRupiah(item.amount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Card for Rule 3: Status Pencairan (Withdrawal)
 * Color Badges:
 * - Menunggu: Kuning (Container 0xFFFEF3C7, Text 0xFFD97706, Border 0xFFFDE68A)
 * - Diproses: Biru (Container 0xFFE0F2FE, Text 0xFF0284C7, Border 0xFFBAE6FD)
 * - Berhasil: Hijau (Container 0xFFD1FAE5, Text 0xFF059669, Border 0xFFA7F3D0)
 * - Ditolak: Merah (Container 0xFFFEE2E2, Text 0xFFDC2626, Border 0xFFFECACA), with reason underneath.
 */
@Composable
private fun WithdrawalHistoryCard(item: WithdrawalHistoryItem) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("withdrawal_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = 3.dp,
        fillAlphaTop = 0.52f,
        fillAlphaBottom = 0.28f,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.35f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF0D47A1),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Penarikan ${item.id}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.bankName} (${item.accountNumberMasked}) • ${item.date}",
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )
                    }
                }

                // Withdrawal Status Badge
                WithdrawalStatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nominal Ditransfer",
                    fontSize = 12.sp,
                    color = KubeTextMuted
                )
                Text(
                    text = formatRupiah(item.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
            }

            // If REJECTED, display explicit rejection reason underneath (Rule 3)
            if (item.status == WithdrawalStatus.REJECTED && item.rejectionReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rejection_reason_box_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Alasan Ditolak",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Alasan Penolakan:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = item.rejectionReason,
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Status Badge for Withdrawal states:
 * - Menunggu: Kuning (Container 0xFFFEF3C7, Text 0xFFD97706, Border 0xFFFDE68A)
 * - Diproses: Biru (Container 0xFFE0F2FE, Text 0xFF0284C7, Border 0xFFBAE6FD)
 * - Berhasil: Hijau (Container 0xFFD1FAE5, Text 0xFF059669, Border 0xFFA7F3D0)
 * - Ditolak: Merah (Container 0xFFFEE2E2, Text 0xFFDC2626, Border 0xFFFECACA)
 */
@Composable
private fun WithdrawalStatusBadge(status: WithdrawalStatus) {
    val (bgColor, textColor, borderColor, icon) = when (status) {
        WithdrawalStatus.WAITING -> Quadruple(
            Color(0xFFFEF3C7), // Kuning
            Color(0xFFD97706),
            Color(0xFFFDE68A),
            Icons.Filled.AccessTime
        )
        WithdrawalStatus.PROCESSING -> Quadruple(
            Color(0xFFE0F2FE), // Biru
            Color(0xFF0284C7),
            Color(0xFFBAE6FD),
            Icons.Filled.Sync
        )
        WithdrawalStatus.SUCCESS -> Quadruple(
            Color(0xFFD1FAE5), // Hijau
            Color(0xFF059669),
            Color(0xFFA7F3D0),
            Icons.Filled.CheckCircle
        )
        WithdrawalStatus.REJECTED -> Quadruple(
            Color(0xFFFEE2E2), // Merah
            Color(0xFFDC2626),
            Color(0xFFFECACA),
            Icons.Filled.ErrorOutline
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * ATURAN 2: Alur UI Pengajuan Penarikan Dana (Withdrawal Modal Bottom Sheet)
 * - Munculkan ModalBottomSheet untuk konfirmasi jumlah nominal yang ditarik dan rincian rekening tujuan (yang di-masking: BCA ****2831).
 * - Saat dikonfirmasi, ubah state tombol menjadi loading (CircularProgressIndicator).
 * - Setelah berhasil insert ke Supabase, tutup bottom sheet dan munculkan dialog/snackbar:
 *   'Pengajuan penarikan dana berhasil dikirim. Admin akan memproses transfer Anda segera.'
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WithdrawalModalBottomSheet(
    availableBalance: Long,
    bankAccountMasked: String,
    accountHolder: String,
    onDismiss: () -> Unit,
    onConfirmWithdraw: suspend (Long) -> Result<WithdrawalHistoryItem>
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .imePadding()
            .testTag("sheet_tarik_dana")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Konfirmasi Penarikan Dana",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "Pengajuan manual ke Web Admin Kube",
                        fontSize = 12.sp,
                        color = KubeTextMuted
                    )
                }
            }

            // Rincian Rekening Tujuan (Masking BCA)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = "BCA",
                            tint = Color(0xFF0D47A1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rekening Tujuan Transfer (BCA)",
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "BCA • $bankAccountMasked",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "a.n. $accountHolder",
                            fontSize = 12.sp,
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE8F5E9)) {
                        Text(
                            text = "Terdaftar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Batas Saldo Info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Saldo Siap Cair: ${formatRupiah(availableBalance)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                }
            }

            // Input Field
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    if (!isLoading) {
                        val digits = input.filter { it.isDigit() }
                        amountInput = digits
                        errorMessage = null
                    }
                },
                enabled = !isLoading,
                label = { Text("Nominal Penarikan (Rp)") },
                placeholder = { Text("Contoh: 500000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { { Text(it, color = Color(0xFFDC2626)) } },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KubeBlueBright,
                    unfocusedBorderColor = Color.LightGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_nominal_penarikan")
            )

            // Quick nominal buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickAmountChip(
                    label = "Rp 100rb",
                    onClick = { if (!isLoading) { amountInput = "100000"; errorMessage = null } },
                    modifier = Modifier.weight(1f)
                )
                QuickAmountChip(
                    label = "Rp 500rb",
                    onClick = { if (!isLoading) { amountInput = "500000"; errorMessage = null } },
                    modifier = Modifier.weight(1f)
                )
                QuickAmountChip(
                    label = "Semua Saldo",
                    onClick = { if (!isLoading) { amountInput = availableBalance.toString(); errorMessage = null } },
                    modifier = Modifier.weight(1f)
                )
            }

            // Notice Manual Admin Transfer
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Transfer dilakukan secara manual oleh Web Admin setelah memverifikasi buku tabungan. Status saldo akan berstatus 'Menunggu' hingga dana masuk.",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button with Loading Indicator
            Button(
                onClick = {
                    val amount = amountInput.toLongOrNull() ?: 0L
                    when {
                        amount <= 0 -> {
                            errorMessage = "Masukkan nominal lebih dari Rp 0"
                        }
                        amount > availableBalance -> {
                            errorMessage = "Saldo tersedia tidak mencukupi"
                        }
                        amount < 50000 -> {
                            errorMessage = "Minimal penarikan adalah Rp 50.000"
                        }
                        else -> {
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = onConfirmWithdraw(amount)
                                isLoading = false
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message ?: "Gagal memproses penarikan"
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_konfirmasi_penarikan")
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Memproses ke Database...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Kirim Pengajuan Penarikan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAmountChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = KubeTextDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private fun formatRupiah(amount: Long): String {
    return "Rp ${"%,d".format(amount).replace(',', '.')}"
}
