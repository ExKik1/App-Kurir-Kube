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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.ZoomIn
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
import com.example.core.model.DeliveryOrder
import com.example.core.model.DeliveryStatus
import com.example.core.network.CourierSupabaseService
import com.example.core.network.OrderPaymentMeta
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryHistoryDetailBottomSheet(
    order: DeliveryOrder,
    meta: OrderPaymentMeta? = CourierSupabaseService.instance.getOrderMeta(order.id),
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val resolvedMeta = meta ?: CourierSupabaseService.instance.getOrderMeta(order.id)
    var fullScreenPreviewUrl by remember { mutableStateOf<String?>(null) }
    var fullScreenPreviewTitle by remember { mutableStateOf("") }

    val podPhotoUrl = resolvedMeta?.podPhotoUrl?.takeIf { it.isNotBlank() && it != "null" }
        ?: order.proofOfDelivery?.photoUri?.takeIf { it.isNotBlank() && it != "null" }
    val codCashPhotoUrl = resolvedMeta?.codCashPhotoUrl?.takeIf { it.isNotBlank() && it != "null" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF4F8FC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("sheet_delivery_history_detail")
    ) {
        DeliveryHistoryDetailContent(
            order = order,
            meta = resolvedMeta,
            onOpenFullScreenProof = {
                if (podPhotoUrl != null) {
                    fullScreenPreviewTitle = "Bukti Pengiriman • #${order.id}"
                    fullScreenPreviewUrl = podPhotoUrl
                }
            },
            onOpenFullScreenCodProof = {
                if (codCashPhotoUrl != null) {
                    fullScreenPreviewTitle = "Bukti Uang Tunai (COD) • #${order.id}"
                    fullScreenPreviewUrl = codCashPhotoUrl
                }
            }
        )
    }

    fullScreenPreviewUrl?.let { previewUrl ->
        Dialog(
            onDismissRequest = { fullScreenPreviewUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .testTag("dialog_fullscreen_pod_photo")
            ) {
                KubeSupabaseImage(
                    imageUrl = previewUrl,
                    contentDescription = fullScreenPreviewTitle,
                    contentScale = ContentScale.Fit,
                    backgroundColor = Color.Transparent,
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
                        text = fullScreenPreviewTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { fullScreenPreviewUrl = null },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .testTag("btn_close_pod_preview")
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
internal fun DeliveryHistoryDetailContent(
    order: DeliveryOrder,
    meta: OrderPaymentMeta?,
    onOpenFullScreenProof: () -> Unit = {},
    onOpenFullScreenCodProof: () -> Unit = {}
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
    val podPhotoUrl = meta?.podPhotoUrl?.takeIf { it.isNotBlank() && it != "null" }
        ?: order.proofOfDelivery?.photoUri?.takeIf { it.isNotBlank() && it != "null" }
    val codCashPhotoUrl = meta?.codCashPhotoUrl?.takeIf { it.isNotBlank() && it != "null" }
    val rawNotes = meta?.deliveryNotes?.takeIf { it.isNotBlank() && it != "null" }
        ?: order.proofOfDelivery?.notes?.takeIf { it.isNotBlank() && it != "null" }
        ?: order.notes?.takeIf { it.isNotBlank() && it != "null" }
    val deliveryNotes = rawNotes
        ?.replace(Regex("""\|?\s*Bukti Uang COD:\s*https?://\S+"""), "")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HEADER CARD
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFE0F2FE).copy(alpha = 0.55f)
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
                                    .size(40.dp)
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
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("detail_history_customer")
                                )
                                Text(
                                    text = "#${order.id}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KubeTextMuted,
                                    modifier = Modifier.testTag("detail_history_code")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFCBD5E1).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = order.address,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }

                    if (order.phoneNumber.isNotBlank() && order.phoneNumber != "-") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                tint = KubeBlueBright,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = order.phoneNumber,
                                fontSize = 12.sp,
                                color = KubeTextMuted
                            )
                        }
                    }
                }
            }
        }

        // 2. RINCIAN BARANG ES
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Inbox,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rincian Barang Es",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                }
                Text(
                    text = order.itemDescription,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A),
                    lineHeight = 18.sp
                )
            }
        }

        // 3. GRID RINGKASAN KEUANGAN & PENERIMA
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = paymentMethodLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KubeTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatHistoryDetailRupiah(totalAmount),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Komisi Kurir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+ ${formatHistoryDetailRupiah(commissionAmount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Diterima: $recipientName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = null,
                            tint = KubeTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = completedTime,
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }

        // 4. CATATAN PENGANTARAN
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("detail_history_notes_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Catatan Pengantaran",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                }
                Text(
                    text = deliveryNotes ?: "Tidak ada catatan pengantaran",
                    fontSize = 13.sp,
                    color = if (deliveryNotes != null) Color(0xFF0F172A) else KubeTextMuted,
                    lineHeight = 18.sp
                )
            }
        }

        // 5. FOTO BUKTI PENGIRIMAN (PoD)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Foto Bukti Pengiriman (PoD)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    if (podPhotoUrl != null) {
                        Text(
                            text = "Ketuk untuk perbesar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KubeBlueBright,
                            modifier = Modifier.clickable(onClick = onOpenFullScreenProof)
                        )
                    }
                }

                if (podPhotoUrl != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 170.dp, max = 250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable(onClick = onOpenFullScreenProof)
                            .testTag("img_history_pod_photo"),
                        contentAlignment = Alignment.Center
                    ) {
                        KubeSupabaseImage(
                            imageUrl = podPhotoUrl,
                            contentDescription = "Foto Bukti Pengiriman #${order.id}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 170.dp, max = 250.dp)
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
                                    contentDescription = "Perbesar Foto PoD",
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
                            .testTag("empty_history_pod_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                                text = "Foto bukti pengiriman belum tersedia.",
                                fontSize = 13.sp,
                                color = KubeTextMuted
                            )
                        }
                    }
                }
            }
        }

        // 6. KARTU TERPISAH: BUKTI UANG TUNAI (COD)
        if (isCod || !codCashPhotoUrl.isNullOrBlank()) {
            KubeGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_history_cod_cash"),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            KubeGlassIconBox(
                                icon = Icons.Filled.Payments,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                boxSize = 36.dp,
                                iconSize = 18.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bukti Uang Tunai (COD)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = formatHistoryDetailRupiah(totalAmount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        KubeGlassPill(
                            text = "Lunas Tunai",
                            textColor = Color(0xFF059669)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lampiran Foto Uang Tunai",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E3A4C)
                        )
                        if (!codCashPhotoUrl.isNullOrBlank()) {
                            Text(
                                text = "Ketuk untuk perbesar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubeBlueBright,
                                modifier = Modifier
                                    .clickable(onClick = onOpenFullScreenCodProof)
                                    .testTag("btn_zoom_cod_photo")
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F5F9))
                            .then(
                                if (!codCashPhotoUrl.isNullOrBlank()) {
                                    Modifier.clickable(onClick = onOpenFullScreenCodProof)
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("img_history_cod_photo"),
                        contentAlignment = Alignment.Center
                    ) {
                        KubeSupabaseImage(
                            imageUrl = codCashPhotoUrl,
                            contentDescription = "Foto Bukti Uang Tunai COD #${order.id}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                        if (!codCashPhotoUrl.isNullOrBlank()) {
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
                                        contentDescription = "Perbesar Foto Uang COD",
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
                    }
                }
            }
        }
    }
}

private fun formatHistoryDetailRupiah(amount: Long): String {
    return "Rp ${"%,d".format(amount).replace(',', '.')}"
}
