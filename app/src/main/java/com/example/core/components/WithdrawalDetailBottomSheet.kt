package com.example.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.core.model.WithdrawalHistoryItem
import com.example.core.model.WithdrawalStatus
import com.example.core.network.CourierSupabaseService
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawalDetailBottomSheet(
    item: WithdrawalHistoryItem,
    onDismiss: () -> Unit,
    onConfirmedSuccess: (WithdrawalHistoryItem) -> Unit,
    onConfirmReceived: suspend (String) -> Result<Unit> = { code ->
        CourierSupabaseService.instance.confirmWithdrawalReceived(code)
    }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFullScreenProof by remember { mutableStateOf(false) }

    val proofUrl = item.transferProofUrl?.takeIf { it.isNotBlank() && it != "null" }
    val showConfirmButton = item.status == WithdrawalStatus.PROCESSING ||
        (proofUrl != null && item.status != WithdrawalStatus.SUCCESS && item.status != WithdrawalStatus.REJECTED)

    ModalBottomSheet(
        onDismissRequest = { if (!isConfirming) onDismiss() },
        sheetState = sheetState,
        containerColor = Color(0xFFF4F8FC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("sheet_withdrawal_detail")
    ) {
        WithdrawalDetailContent(
            item = item,
            proofUrl = proofUrl,
            showConfirmButton = showConfirmButton,
            isConfirming = isConfirming,
            errorMessage = errorMessage,
            onOpenFullScreenProof = { showFullScreenProof = true },
            onConfirmClick = {
                if (!isConfirming) {
                    isConfirming = true
                    errorMessage = null
                    coroutineScope.launch {
                        val res = onConfirmReceived(item.id)
                        isConfirming = false
                        if (res.isSuccess) {
                            onConfirmedSuccess(item)
                        } else {
                            errorMessage = res.exceptionOrNull()?.message
                                ?: "Gagal mengonfirmasi penerimaan dana. Silakan coba lagi."
                        }
                    }
                }
            }
        )
    }

    if (showFullScreenProof && proofUrl != null) {
        Dialog(
            onDismissRequest = { showFullScreenProof = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .testTag("dialog_fullscreen_transfer_proof")
            ) {
                AsyncImage(
                    model = proofUrl,
                    contentDescription = "Pratinjau Penuh Bukti Transfer ${item.id}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 64.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bukti Transfer • ${item.id}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showFullScreenProof = false },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .testTag("btn_close_proof_preview")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup Pratinjau",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WithdrawalDetailContent(
    item: WithdrawalHistoryItem,
    proofUrl: String?,
    showConfirmButton: Boolean,
    isConfirming: Boolean,
    errorMessage: String?,
    onOpenFullScreenProof: () -> Unit,
    onConfirmClick: () -> Unit
) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. HEADER: Kode Penarikan, Status Badge, Nominal Ditransfer
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF).copy(alpha = 0.92f),
                                    Color(0xFFE0F2FE).copy(alpha = 0.65f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Kode Penarikan",
                                        fontSize = 11.sp,
                                        color = KubeTextMuted
                                    )
                                    Text(
                                        text = item.id,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KubeTextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("detail_withdrawal_id")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            WithdrawalDetailStatusBadge(status = item.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Nominal Ditransfer",
                            fontSize = 12.sp,
                            color = KubeTextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatRupiahDetail(item.amount),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            modifier = Modifier.testTag("detail_withdrawal_amount")
                        )
                    }
                }
            }

            // 2. INFORMASI REKENING & WAKTU
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Informasi Rekening & Waktu",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    DetailKeyValueRow("Bank Tujuan", item.bankName.ifBlank { "-" })
                    DetailKeyValueRow("Nomor Rekening", item.accountNumberMasked.ifBlank { "-" })
                    DetailKeyValueRow("Atas Nama", item.recipientName.ifBlank { "-" })
                    DetailKeyValueRow("Tanggal Pengajuan", item.date.ifBlank { "-" })
                    DetailKeyValueRow(
                        "Waktu Diproses",
                        item.processedAt ?: "Menunggu proses Admin"
                    )
                    DetailKeyValueRow(
                        "Admin Pemroses",
                        item.processedByName ?: "Admin Kube"
                    )
                }
            }

            // Rejection reason if applicable
            if (item.status == WithdrawalStatus.REJECTED && !item.rejectionReason.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Alasan Penolakan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = item.rejectionReason,
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }

            // 3. SEKSI BUKTI TRANSFER ADMIN
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bukti Transfer Admin",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        if (proofUrl != null) {
                            Text(
                                text = "Ketuk untuk perbesar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubeBlueBright
                            )
                        }
                    }

                    if (proofUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp, max = 260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .clickable(onClick = onOpenFullScreenProof)
                                .testTag("img_transfer_proof"),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = proofUrl,
                                contentDescription = "Bukti Transfer Penarikan ${item.id}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 260.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ZoomIn,
                                        contentDescription = "Perbesar Bukti Transfer",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Perbesar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_transfer_proof_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = KubeTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Bukti transfer belum diunggah oleh Admin.",
                                    fontSize = 13.sp,
                                    color = KubeTextMuted
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        fontSize = 12.sp,
                        color = Color(0xFFB91C1C),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 4. TOMBOL VERIFIKASI KURIR
            if (showConfirmButton) {
                Button(
                    onClick = onConfirmClick,
                    enabled = !isConfirming,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_confirm_withdrawal_received")
                ) {
                    if (isConfirming) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Memverifikasi Dana...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Konfirmasi Uang Sudah Diterima",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

@Composable
private fun DetailKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = KubeTextMuted,
            modifier = Modifier.weight(0.45f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KubeTextDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun WithdrawalDetailStatusBadge(status: WithdrawalStatus) {
    val (bgColor, textColor, borderColor, icon) = when (status) {
        WithdrawalStatus.WAITING -> DetailBadgeStyle(
            Color(0xFFFEF3C7),
            Color(0xFFD97706),
            Color(0xFFFDE68A),
            Icons.Filled.AccessTime
        )
        WithdrawalStatus.PROCESSING -> DetailBadgeStyle(
            Color(0xFFDBEAFE),
            Color(0xFF1D4ED8),
            Color(0xFF93C5FD),
            Icons.Filled.Verified
        )
        WithdrawalStatus.SUCCESS -> DetailBadgeStyle(
            Color(0xFFD1FAE5),
            Color(0xFF059669),
            Color(0xFFA7F3D0),
            Icons.Filled.CheckCircle
        )
        WithdrawalStatus.REJECTED -> DetailBadgeStyle(
            Color(0xFFFEE2E2),
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

private data class DetailBadgeStyle(
    val bg: Color,
    val text: Color,
    val border: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun formatRupiahDetail(amount: Long): String {
    return "Rp ${"%,d".format(amount).replace(',', '.')}"
}
