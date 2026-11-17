package com.example.features.delivery

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.model.DeliveryOrder
import com.example.core.network.CourierSupabaseService
import com.example.core.network.OrderPaymentMeta
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

/**
 * Proof of Delivery (PoD) ModalBottomSheet.
 *
 * Requirements:
 * 1. Kamera / Upload Foto: Photo Picker for evidence of delivered ice.
 * 2. Nama Penerima: Mandatory TextFormField with validation.
 * 3. Konfirmasi Jumlah Barang: Stepper counter for confirming ice quantity.
 * 4. Catatan Tambahan: Optional TextFormField.
 * 5. Logika Submit: Changes status to 'Menunggu Verifikasi Admin' and shows confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofOfDeliveryBottomSheet(
    order: DeliveryOrder,
    orderMeta: OrderPaymentMeta? = remember(order.id) { CourierSupabaseService.instance.getOrderMeta(order.id) },
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    onSubmitProof: (
        recipientName: String,
        photoUri: String?,
        confirmedQuantity: Int,
        notes: String?,
        isCodPaymentConfirmed: Boolean
    ) -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var confirmedQuantity by remember { mutableIntStateOf(order.defaultQuantity) }
    var notes by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    val isCodOrder = orderMeta?.isCodOrder ?: order.isCod
    val codTotalAmount = orderMeta?.totalAmount?.takeIf { it > 0L } ?: order.effectiveTotalAmount
    val codUnitPrice = orderMeta?.unitPrice?.takeIf { it > 0L } ?: order.effectiveUnitPrice
    var isCodCashConfirmed by remember(order.id) { mutableStateOf(false) }

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val isNameError = hasAttemptedSubmit && recipientName.trim().isBlank()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFFF5F7FA),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bukti Pengiriman (PoD)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "Lengkapi bukti serah terima es batu ke penerima",
                        fontSize = 12.sp,
                        color = KubeTextMuted
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = KubeTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Order Info Glass Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)), RoundedCornerShape(16.dp)),
                color = Color.White.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF0096C7), Color(0xFF00B4DB))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = order.id,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0288D1)
                            )
                            Text(
                                text = "•",
                                fontSize = 11.sp,
                                color = KubeTextMuted
                            )
                            Text(
                                text = order.customerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeTextDark
                            )
                        }
                        Text(
                            text = "Pesanan: ${order.itemDescription}",
                            fontSize = 12.sp,
                            color = KubeTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. KAMERA / UPLOAD FOTO
            Text(
                text = "Foto Bukti Penyerahan Es Batu",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = KubeTextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ambil foto es batu yang sudah diletakkan atau diterima oleh pihak toko.",
                fontSize = 11.sp,
                color = KubeTextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedPhotoUri != null) {
                // Photo Preview with Change/Remove actions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(BorderStroke(1.5.dp, Color(0xFF0096C7).copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
                        .background(Color.White)
                ) {
                    AsyncImage(
                        model = selectedPhotoUri,
                        contentDescription = "Bukti Foto Es Batu",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay top buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.9f),
                            contentColor = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Foto Terlampir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Ubah Foto",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { selectedPhotoUri = null }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Foto",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Photo Placeholder Card with picker buttons
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            BorderStroke(
                                width = 1.2.dp,
                                color = Color(0xFF0096C7).copy(alpha = 0.35f)
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("upload_photo_container"),
                    color = Color.White.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(Color(0xFFE0F7FA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ambil Foto",
                                tint = Color(0xFF0096C7),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Ambil Foto / Pilih dari Galeri",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )

                        Text(
                            text = "Mendukung kamera & galeri foto perangkat",
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0096C7),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buka Picker", fontSize = 12.sp)
                            }

                            // Quick test helper for emulator / offline testing
                            Button(
                                onClick = {
                                    // Use a sample asset URI for instant testing
                                    selectedPhotoUri = Uri.parse("android.resource://com.example/drawable/ic_launcher_foreground")
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE2E8F0),
                                    contentColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("Gunakan Sampel", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. NAMA PENERIMA (WAJIB)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Nama Penerima",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
                Text(
                    text = "* (Wajib Diisi)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recipient_name_input"),
                placeholder = {
                    Text(
                        text = "Contoh: Bpk. Ahmad (Pemilik Toko)",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isNameError) Color(0xFFE53935) else Color(0xFF0096C7)
                    )
                },
                isError = isNameError,
                supportingText = {
                    if (isNameError) {
                        Text(
                            text = "Nama penerima wajib diisi sebelum mengirim laporan.",
                            color = Color(0xFFE53935),
                            fontSize = 11.sp
                        )
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF1E293B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0096C7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    errorBorderColor = Color(0xFFE53935),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. KONFIRMASI JUMLAH BARANG
            Text(
                text = "Konfirmasi Jumlah Barang Diterima",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = KubeTextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pastikan jumlah fisik es batu sesuai dengan pesanan pelanggan.",
                fontSize = 11.sp,
                color = KubeTextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(16.dp)),
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = order.itemDescription,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Text(
                            text = "Target pesanan: ${order.defaultQuantity} unit",
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )
                    }

                    // Counter Stepper [-] [count] [+]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (confirmedQuantity > 1) Color(0xFFE0F7FA) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(enabled = confirmedQuantity > 1) {
                                    if (confirmedQuantity > 1) confirmedQuantity--
                                }
                                .testTag("qty_minus_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Kurang",
                                    tint = if (confirmedQuantity > 1) Color(0xFF0096C7) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$confirmedQuantity",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0096C7)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE0F7FA),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { confirmedQuantity++ }
                                .testTag("qty_plus_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    tint = Color(0xFF0096C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. CATATAN TAMBAHAN (OPSIONAL)
            Text(
                text = "Catatan Tambahan (Opsional)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = KubeTextDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pod_notes_input"),
                placeholder = {
                    Text(
                        text = "Misal: Es batu disimpan langsung di freezer belakang toko.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = Color(0xFF0096C7)
                    )
                },
                maxLines = 3,
                textStyle = TextStyle(
                    color = Color(0xFF1E293B),
                    fontSize = 13.sp
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0096C7),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Khusus pesanan COD: Wajib konfirmasi penerimaan uang tunai sebelum tombol Submit aktif
            if (isCodOrder) {
                val formattedCodTotal = formatPodRupiah(codTotalAmount)
                val formattedUnitPrice = formatPodRupiah(codUnitPrice)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isCodCashConfirmed = !isCodCashConfirmed }
                        .testTag("cod_cash_confirmation_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCodCashConfirmed) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (isCodCashConfirmed) Color(0xFF4CAF50) else Color(0xFFF59E0B)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCodCashConfirmed) Color(0xFF2E7D32).copy(alpha = 0.14f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (isCodCashConfirmed) Color(0xFF2E7D32) else Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Tagihan Tunai COD",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KubeTextDark
                                )
                                Text(
                                    text = "${order.itemDescription} (${formattedUnitPrice}/unit) · Total $formattedCodTotal",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCodCashConfirmed) Color(0xFF1B5E20) else Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("cod_cash_confirmation_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isCodCashConfirmed,
                                onCheckedChange = { isCodCashConfirmed = it },
                                modifier = Modifier.testTag("cod_cash_confirmation_checkbox"),
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF2E7D32),
                                    uncheckedColor = Color(0xFFD97706),
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Saya konfirmasi telah menerima uang tunai sebesar $formattedCodTotal dari pelanggan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubeTextDark,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Info Notice: Direct completion logic
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE8F5E9),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pesanan akan langsung diselesaikan dan otomatis masuk ke tab Riwayat / Selesai.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20),
                        lineHeight = 15.sp
                    )
                }
            }

            if (isCodOrder && !isCodCashConfirmed) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Centang konfirmasi penerimaan uang tunai COD di atas untuk mengaktifkan tombol Submit PoD.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val isSubmitEnabled = !isCodOrder || isCodCashConfirmed

            // 5. TOMBOL SUBMIT: 'Selesaikan & Kirim PoD'
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (recipientName.trim().isNotBlank() && isSubmitEnabled) {
                        onSubmitProof(
                            recipientName.trim(),
                            selectedPhotoUri?.toString(),
                            confirmedQuantity,
                            notes.trim().ifBlank { null },
                            isCodCashConfirmed
                        )
                    }
                },
                enabled = isSubmitEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_pod_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32), // Green completion color
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF94A3B8).copy(alpha = 0.55f),
                    disabledContentColor = Color.White.copy(alpha = 0.85f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Selesaikan Pengiriman",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatPodRupiah(amount: Long): String {
    val formatted = String.format(java.util.Locale("in", "ID"), "%,d", amount).replace(',', '.')
    return "Rp $formatted"
}
