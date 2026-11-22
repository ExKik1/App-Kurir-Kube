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
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.BankAccountChangeBottomSheet
import com.example.core.components.BankVerificationStatusPill
import com.example.core.components.DarkObsidianGlassCard
import com.example.core.components.DbBodyText
import com.example.core.components.DbCurrencyOrMetricText
import com.example.core.components.DbTitleText
import com.example.core.components.KubeFeedbackManager
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassIconBox
import com.example.core.components.KubeGlassPill
import com.example.core.components.KubeMutedText
import com.example.core.components.KubePrimaryText
import com.example.core.components.TopCourierBar
import com.example.core.components.WithdrawalDetailBottomSheet
import com.example.core.components.kubeLightTextFieldColors
import com.example.core.model.CommissionHistoryItem
import com.example.core.model.WithdrawalHistoryItem
import com.example.core.model.WithdrawalStatus
import com.example.core.model.maskAccountNumber
import com.example.core.network.CourierSupabaseService
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
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialCourier = remember(repository) { repository.courier.value }
    val initialWallet = remember(repository) { repository.walletBalance.value }
    val initialCommissions = remember(repository) { repository.commissionHistory.value }
    val initialWithdrawals = remember(repository) { repository.withdrawalHistory.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }

    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val walletBalance by supabaseService.streamWalletSummary(courierProfileId).collectAsState(initial = initialWallet)
    val commissionList by supabaseService.streamWalletTransactions(courierProfileId).collectAsState(initial = initialCommissions)
    val withdrawalList by supabaseService.streamWithdrawals(courierProfileId).collectAsState(initial = initialWithdrawals)
    val deliveries by supabaseService.streamMyDeliveries(courierProfileId).collectAsState(initial = emptyList())
    val notifications by supabaseService.streamNotifications(courierProfileId).collectAsState(initial = emptyList())
    val bankAccounts by supabaseService.bankAccounts.collectAsState()
    val primaryBank = remember(bankAccounts) {
        bankAccounts.firstOrNull { it.isPrimary } ?: bankAccounts.firstOrNull()
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Riwayat Komisi, 1: Riwayat Pencairan
    var showBankChangeSheet by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var selectedWithdrawalDetail by remember { mutableStateOf<WithdrawalHistoryItem?>(null) }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
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
                    onAvatarClick = onNavigateToProfile,
                    onNotifClick = onNavigateToNotifications
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
                        color = Color(0xFF1E3A4C)
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
                    // Card 1: Saldo Tertunda
                    BalanceMetricCard(
                        title = "Saldo Tertunda",
                        amountText = formatRupiah(walletBalance.pendingBalance),
                        statusHint = "Menunggu",
                        icon = Icons.Filled.AccessTime,
                        accentColor = Color(0xFFD97706),
                        isHighlighted = false,
                        tag = "card_saldo_tertunda",
                        modifier = Modifier.weight(1f)
                    )

                    // Card 2: Saldo Tersedia
                    BalanceMetricCard(
                        title = "Saldo Tersedia",
                        amountText = formatRupiah(walletBalance.availableBalance),
                        statusHint = "Siap cair",
                        icon = Icons.Filled.CheckCircle,
                        accentColor = Color(0xFF059669),
                        isHighlighted = true,
                        tag = "card_saldo_tersedia",
                        modifier = Modifier.weight(1f)
                    )

                    // Card 3: Saldo Dicairkan
                    BalanceMetricCard(
                        title = "Saldo Dicairkan",
                        amountText = formatRupiah(walletBalance.withdrawnBalance),
                        statusHint = "Total ditarik",
                        icon = Icons.Filled.Payment,
                        accentColor = Color(0xFF0284C7),
                        isHighlighted = false,
                        tag = "card_saldo_dicairkan",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Tarik Saldo Tersedia
                DarkObsidianGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("action_withdraw_banner"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
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
                            DbCurrencyOrMetricText(
                                text = formatRupiah(walletBalance.availableBalance),
                                fontSize = 20.sp,
                                color = KubeCyan
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

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
                KubeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("card_rekening_bca"),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KubeGlassIconBox(
                                icon = Icons.Filled.AccountBalance,
                                contentDescription = "Bank BCA",
                                tint = Color(0xFF0D47A1),
                                boxSize = 46.dp,
                                iconSize = 22.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                val bankLabel = primaryBank?.bankName?.takeIf { it.isNotBlank() && it != "-" }
                                    ?: courier.bankName.ifBlank { "Bank" }
                                val rawAccountNumber = primaryBank?.accountNumber?.takeIf { it.isNotBlank() && it != "-" }
                                    ?: courier.bankAccountNumber
                                val accountMasked = if (rawAccountNumber.isBlank()) {
                                    "Belum diatur"
                                } else {
                                    maskAccountNumber(rawAccountNumber)
                                }
                                val holderName = primaryBank?.accountHolder?.takeIf { it.isNotBlank() && it != "-" }
                                    ?: courier.bankAccountHolder.ifBlank { courier.name.ifBlank { "-" } }
                                DbTitleText(
                                    text = "$bankLabel • $accountMasked"
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                DbBodyText(
                                    text = "a.n. $holderName",
                                    maxLines = 1,
                                    color = Color(0xFF1E3A4C)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            BankVerificationStatusPill(
                                verificationStatus = primaryBank?.verificationStatus
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Ajukan Perubahan Rekening Button
                        OutlinedButton(
                            onClick = { showBankChangeSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF0096C7).copy(alpha = 0.40f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.55f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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
                                text = "Ajukan Perubahan Rekening",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
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
                KubeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                        if (commissionList.isEmpty()) {
                            KubeGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(
                                    text = "Belum ada riwayat komisi untuk ${deliveries.size} pengiriman & ${notifications.size} notifikasi.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E3A4C)
                                )
                            }
                        } else {
                            commissionList.forEach { commission ->
                                CommissionHistoryCard(item = commission)
                            }
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
                        if (withdrawalList.isEmpty()) {
                            KubeGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Text(
                                    text = "Belum ada pengajuan pencairan dana.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E3A4C)
                                )
                            }
                        } else {
                            withdrawalList.forEach { withdrawal ->
                                WithdrawalHistoryCard(
                                    item = withdrawal,
                                    onClick = { selectedWithdrawalDetail = withdrawal }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // BOTTOM SHEET: AJUKAN PERUBAHAN REKENING KE SUPABASE
        // ==========================================
        if (showBankChangeSheet) {
            val initialBank = primaryBank?.bankName?.takeIf { it.isNotBlank() && it != "-" }
                ?: courier.bankName
            val initialHolder = primaryBank?.accountHolder?.takeIf { it.isNotBlank() && it != "-" }
                ?: courier.bankAccountHolder
            BankAccountChangeBottomSheet(
                initialBankName = initialBank,
                initialAccountHolder = initialHolder,
                onDismiss = { showBankChangeSheet = false },
                onSubmitSuccess = { created ->
                    showBankChangeSheet = false
                    KubeFeedbackManager.showSuccess(
                        "Pengajuan Rekening Terkirim",
                        "Data rekening ${created.bankName} berhasil dikirim dan menunggu verifikasi Admin."
                    )
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
                    val result = try {
                        CourierSupabaseService.instance.requestWithdrawal(
                            courierId = courier.id,
                            amount = amount,
                            bankAccount = courier.bankAccountNumber
                        )
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                    if (result.isSuccess) {
                        showWithdrawDialog = false
                        selectedTab = 1 // Switch to Riwayat Pencairan to show new status
                        KubeFeedbackManager.showSuccess(
                            "Penarikan Dana Diajukan",
                            "Pengajuan pencairan sebesar ${formatRupiah(amount)} sedang diproses Admin."
                        )
                    } else {
                        KubeFeedbackManager.showError(
                            "Penarikan Dana Gagal",
                            result.exceptionOrNull()?.message ?: "Gagal mengirim pengajuan penarikan dana."
                        )
                    }
                    result
                }
            )
        }

        // ==========================================
        // BOTTOM SHEET: RINCIAN & BUKTI TRANSFER PENCAIRAN
        // ==========================================
        selectedWithdrawalDetail?.let { detailItem ->
            WithdrawalDetailBottomSheet(
                item = detailItem,
                onDismiss = { selectedWithdrawalDetail = null },
                onConfirmedSuccess = {
                    selectedWithdrawalDetail = null
                    KubeFeedbackManager.showSuccess(
                        "Pencairan Terverifikasi",
                        "Dana penarikan telah dikonfirmasi masuk ke rekening Anda."
                    )
                }
            )
        }
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
    isHighlighted: Boolean,
    tag: String,
    modifier: Modifier = Modifier
) {
    KubeGlassCard(
        modifier = modifier.testTag(tag),
        shape = RoundedCornerShape(20.dp),
        isHighlighted = isHighlighted,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            KubeGlassIconBox(
                icon = icon,
                contentDescription = title,
                tint = accentColor,
                isHighlighted = isHighlighted,
                boxSize = 36.dp,
                iconSize = 18.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            DbCurrencyOrMetricText(
                text = amountText,
                fontSize = 14.sp,
                color = KubePrimaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            KubeGlassPill(
                text = statusHint,
                textColor = accentColor
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 10.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
    KubeGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("commission_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
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
                    icon = if (item.isTip) Icons.Filled.ThumbUp else Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = if (item.isTip) Color(0xFF059669) else Color(0xFF0284C7),
                    boxSize = 42.dp,
                    iconSize = 20.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    DbTitleText(
                        text = item.title,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    DbBodyText(
                        text = item.subtitle,
                        maxLines = 1,
                        color = Color(0xFF1E3A4C)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            KubeGlassPill(
                text = "+ ${formatRupiah(item.amount)}",
                textColor = Color(0xFF2E7D32)
            )
        }
    }
}

/**
 * Card for Rule 3: Status Pencairan (Withdrawal)
 */
@Composable
private fun WithdrawalHistoryCard(
    item: WithdrawalHistoryItem,
    onClick: () -> Unit = {}
) {
    val hasProof = !item.transferProofUrl.isNullOrBlank() && item.transferProofUrl != "null"

    KubeGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("withdrawal_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
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
                        icon = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF0D47A1),
                        boxSize = 42.dp,
                        iconSize = 20.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        DbTitleText(
                            text = "Penarikan ${item.id}",
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        DbBodyText(
                            text = "${item.bankName} (${item.accountNumberMasked}) • ${item.date}",
                            maxLines = 1,
                            color = Color(0xFF1E3A4C)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

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
                DbBodyText(
                    text = "Nominal Ditransfer",
                    color = Color(0xFF1E3A4C),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                DbCurrencyOrMetricText(
                    text = formatRupiah(item.amount),
                    fontSize = 15.sp,
                    color = KubePrimaryText
                )
            }

            // If REJECTED, display explicit rejection reason underneath (Rule 3)
            if (item.status == WithdrawalStatus.REJECTED && item.rejectionReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                KubeGlassCard(
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rejection_reason_box_${item.id}")
                ) {
                    Row(
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

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasProof) "Lihat Bukti Transfer & Rincian >" else "Lihat Rincian Pengajuan >",
                fontSize = 12.sp,
                fontWeight = if (hasProof) FontWeight.SemiBold else FontWeight.Medium,
                color = if (hasProof) Color(0xFF0284C7) else Color(0xFF1E3A4C),
                modifier = Modifier.testTag("withdrawal_hint_${item.id}")
            )
        }
    }
}

@Composable
private fun WithdrawalStatusBadge(status: WithdrawalStatus) {
    val textColor = when (status) {
        WithdrawalStatus.WAITING -> Color(0xFFD97706)
        WithdrawalStatus.PROCESSING -> Color(0xFF1D4ED8)
        WithdrawalStatus.SUCCESS -> Color(0xFF059669)
        WithdrawalStatus.REJECTED -> Color(0xFFDC2626)
    }

    KubeGlassPill(
        text = status.label,
        textColor = textColor
    )
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
                KubeGlassIconBox(
                    icon = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    isHighlighted = true,
                    boxSize = 48.dp,
                    iconSize = 24.dp
                )
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
            KubeGlassCard(
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KubeGlassIconBox(
                        icon = Icons.Filled.AccountBalance,
                        contentDescription = "BCA",
                        tint = Color(0xFF0D47A1),
                        boxSize = 40.dp,
                        iconSize = 20.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rekening Tujuan Transfer (BCA)",
                            fontSize = 11.sp,
                            color = Color(0xFF1E3A4C)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        DbTitleText(text = "BCA • $bankAccountMasked")
                        Spacer(modifier = Modifier.height(1.dp))
                        DbBodyText(
                            text = "a.n. $accountHolder",
                            maxLines = 1,
                            color = Color(0xFF0284C7)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    KubeGlassPill(
                        text = "Terdaftar",
                        textColor = Color(0xFF2E7D32)
                    )
                }
            }

            // Batas Saldo Info
            KubeGlassCard(
                shape = RoundedCornerShape(14.dp),
                isHighlighted = true,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DbBodyText(
                        text = "Saldo Siap Cair:",
                        color = Color(0xFF065F46),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DbCurrencyOrMetricText(
                        text = formatRupiah(availableBalance),
                        fontSize = 14.sp,
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
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                shape = RoundedCornerShape(14.dp),
                colors = kubeLightTextFieldColors(),
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
            KubeGlassCard(
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
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
    KubeGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
        onClick = onClick
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = KubeTextDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatRupiah(amount: Long): String {
    return "Rp ${"%,d".format(amount).replace(',', '.')}"
}
