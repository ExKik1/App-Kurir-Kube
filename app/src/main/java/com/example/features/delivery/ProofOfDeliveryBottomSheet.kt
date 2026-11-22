package com.example.features.delivery

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.components.DbBodyText
import com.example.core.components.DbStatusPill
import com.example.core.components.DbTitleText
import com.example.core.components.DbCurrencyOrMetricText
import com.example.core.components.KubeFeedbackManager
import com.example.core.components.KubeImageUtils
import com.example.core.components.KubePrimaryText
import com.example.core.components.KubeSupabaseImage
import com.example.core.components.kubeLightTextFieldColors
import com.example.core.model.DeliveryOrder
import com.example.core.network.CourierSupabaseService
import com.example.core.network.OrderPaymentMeta
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
    isOnDuty: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit,
    onSubmitProof: (
        recipientName: String,
        photoUri: String?,
        codCashPhotoUri: String?,
        confirmedQuantity: Int,
        notes: String?,
        isCodPaymentConfirmed: Boolean
    ) -> Unit
) {
    var recipientName by remember(order.id) { mutableStateOf("") }
    var selectedPhotoUri by remember(order.id) { mutableStateOf<Uri?>(null) }
    var codCashPhotoUri by remember(order.id) { mutableStateOf<Uri?>(null) }
    var confirmedQuantity by remember(order.id) { mutableIntStateOf(order.defaultQuantity) }
    var notes by remember(order.id) { mutableStateOf("") }
    var hasAttemptedSubmit by remember(order.id) { mutableStateOf(false) }
    val isCodOrder = orderMeta?.isCodOrder ?: order.isCod
    val codTotalAmount = orderMeta?.totalAmount?.takeIf { it > 0L } ?: order.effectiveTotalAmount
    val codUnitPrice = orderMeta?.unitPrice?.takeIf { it > 0L } ?: order.effectiveUnitPrice
    var isCodCashConfirmed by remember(order.id) { mutableStateOf(false) }

    val context = LocalContext.current
    var pendingCameraTarget by remember(order.id) { mutableStateOf<String?>(null) }
    var pendingPodCameraUri by remember(order.id) { mutableStateOf<Uri?>(null) }
    var pendingCodCameraUri by remember(order.id) { mutableStateOf<Uri?>(null) }

    // Camera TakePicture launchers
    val podCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPodCameraUri != null) {
            selectedPhotoUri = pendingPodCameraUri
        }
    }

    val codCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCodCameraUri != null) {
            codCashPhotoUri = pendingCodCameraUri
        }
    }

    // Runtime CAMERA permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val target = pendingCameraTarget
        pendingCameraTarget = null
        if (isGranted) {
            when (target) {
                "POD" -> {
                    val uri = KubeImageUtils.createTempCameraImageUri(context, prefix = "pod_${order.id}")
                    pendingPodCameraUri = uri
                    podCameraLauncher.launch(uri)
                }
                "COD" -> {
                    val uri = KubeImageUtils.createTempCameraImageUri(context, prefix = "cod_${order.id}")
                    pendingCodCameraUri = uri
                    codCameraLauncher.launch(uri)
                }
            }
        } else {
            KubeFeedbackManager.showWarning(
                "Izin Kamera Diperlukan",
                "Aktifkan izin kamera untuk mengambil foto bukti pengantaran secara langsung."
            )
        }
    }

    val launchCameraForPod: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = KubeImageUtils.createTempCameraImageUri(context, prefix = "pod_${order.id}")
            pendingPodCameraUri = uri
            podCameraLauncher.launch(uri)
        } else {
            pendingCameraTarget = "POD"
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val launchCameraForCod: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = KubeImageUtils.createTempCameraImageUri(context, prefix = "cod_${order.id}")
            pendingCodCameraUri = uri
            codCameraLauncher.launch(uri)
        } else {
            pendingCameraTarget = "COD"
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery GetContent launchers
    val podGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val codGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            codCashPhotoUri = uri
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DbStatusPill(
                                text = "#${order.id}",
                                backgroundColor = Color(0xFFE0F2FE),
                                textColor = Color(0xFF0284C7)
                            )
                            DbTitleText(
                                text = order.customerName,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        DbBodyText(
                            text = "Pesanan: ${order.itemDescription}",
                            maxLines = 2
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
                    KubeSupabaseImage(
                        imageUrl = selectedPhotoUri?.toString(),
                        contentDescription = "Bukti Foto Es Batu",
                        modifier = Modifier.fillMaxWidth().height(180.dp),
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
                                    .clickable { podGalleryLauncher.launch("image/*") }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Ubah Foto dari Galeri",
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
                // Photo Placeholder Card with Camera & Gallery buttons
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
                            text = "Mendukung kamera langsung & galeri foto perangkat",
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = launchCameraForPod,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0096C7),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_pod_camera")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = "Kamera",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kamera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { podGalleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.2.dp, Color(0xFF0096C7)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0096C7)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_pod_gallery")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoLibrary,
                                    contentDescription = "Galeri",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galeri", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                shape = RoundedCornerShape(14.dp),
                colors = kubeLightTextFieldColors()
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
                    Column(modifier = Modifier.weight(1f)) {
                        DbTitleText(
                            text = order.itemDescription,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        DbBodyText(
                            text = "Target pesanan: ${order.defaultQuantity} unit",
                            maxLines = 1,
                            color = KubeTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))

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
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp
                ),
                shape = RoundedCornerShape(14.dp),
                colors = kubeLightTextFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Khusus pesanan COD: Bukti Penerimaan Uang Tunai (COD) + Foto Bukti + Checkbox
            if (isCodOrder) {
                val formattedCodTotal = formatPodRupiah(codTotalAmount)
                val formattedUnitPrice = formatPodRupiah(codUnitPrice)
                val isCodSectionComplete = codCashPhotoUri != null && isCodCashConfirmed
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("cod_cash_confirmation_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isCodSectionComplete) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (isCodSectionComplete) Color(0xFF4CAF50) else Color(0xFFF59E0B)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isCodSectionComplete) Color(0xFF2E7D32).copy(alpha = 0.14f)
                                        else Color(0xFFF59E0B).copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = if (isCodSectionComplete) Color(0xFF2E7D32) else Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                DbTitleText(
                                    text = "Bukti Penerimaan Uang Tunai (COD)",
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                DbBodyText(
                                    text = "${order.itemDescription} ($formattedUnitPrice/unit)",
                                    maxLines = 2,
                                    color = if (isCodSectionComplete) Color(0xFF1B5E20) else Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            DbStatusPill(
                                text = if (isCodSectionComplete) "Lengkap" else "Wajib",
                                backgroundColor = if (isCodSectionComplete) Color(0xFFC8E6C9) else Color(0xFFFDE68A),
                                textColor = if (isCodSectionComplete) Color(0xFF1B5E20) else Color(0xFF92400E)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Ringkasan Nominal Uang Tunai yang Harus Diterima
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nominal Uang Diterima",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KubeTextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                DbCurrencyOrMetricText(
                                    text = formattedCodTotal,
                                    color = if (isCodSectionComplete) Color(0xFF1B5E20) else Color(0xFFB45309),
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input Foto Bukti Uang Tunai COD
                        Text(
                            text = "Foto Bukti Uang Tunai COD *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubePrimaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (codCashPhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.5.dp, Color(0xFF2E7D32).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .testTag("cod_cash_photo_preview")
                            ) {
                                KubeSupabaseImage(
                                    imageUrl = codCashPhotoUri?.toString(),
                                    contentDescription = "Foto Bukti Uang Tunai COD",
                                    modifier = Modifier.fillMaxWidth().height(145.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DbStatusPill(
                                        text = "Bukti Uang Terlampir",
                                        backgroundColor = Color(0xFF2E7D32).copy(alpha = 0.92f),
                                        textColor = Color.White
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            contentColor = Color.White,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { codGalleryLauncher.launch("image/*") }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Ubah Foto Uang COD dari Galeri",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            contentColor = Color.White,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clickable { codCashPhotoUri = null }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Hapus Foto Uang COD",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.55f)),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .testTag("cod_cash_photo_picker"),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.90f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Lampirkan Foto Uang Tunai yang Diterima",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KubePrimaryText
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = launchCameraForCod,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD97706),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_cod_camera")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PhotoCamera,
                                                contentDescription = "Kamera",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Kamera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { codGalleryLauncher.launch("image/*") },
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.2.dp, Color(0xFFD97706)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.White,
                                                contentColor = Color(0xFFB45309)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_cod_gallery")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PhotoLibrary,
                                                contentDescription = "Galeri",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Galeri", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.88f))
                                .clickable { isCodCashConfirmed = !isCodCashConfirmed }
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
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saya sudah menerima uang tunai COD dan melampirkan foto buktinya",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubePrimaryText,
                                lineHeight = 17.sp,
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
                        text = "Pesanan akan langsung diselesaikan, masuk ke riwayat, dan komisi langsung tercatat ke Saldo Siap Cair.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20),
                        lineHeight = 15.sp
                    )
                }
            }

            val isCodRequirementsMet = !isCodOrder || (
                selectedPhotoUri != null &&
                codCashPhotoUri != null &&
                recipientName.trim().isNotBlank() &&
                isCodCashConfirmed
            )

            if (isCodOrder && !isCodRequirementsMet) {
                Spacer(modifier = Modifier.height(10.dp))
                val missingItems = buildList {
                    if (selectedPhotoUri == null) add("foto serah terima es")
                    if (recipientName.trim().isBlank()) add("nama penerima")
                    if (codCashPhotoUri == null) add("foto bukti uang COD")
                    if (!isCodCashConfirmed) add("centang konfirmasi")
                }.joinToString(", ")
                Text(
                    text = "Lengkapi $missingItems untuk mengaktifkan tombol Submit PoD.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB45309)
                )
            }

            if (!isOnDuty) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit PoD terkunci karena status Anda sedang Off-Duty. Aktifkan On-Duty untuk menyelesaikan pengiriman.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val isSubmitEnabled = isOnDuty && (!isCodOrder || isCodRequirementsMet)

            // 5. TOMBOL SUBMIT: 'Submit PoD' / 'Selesaikan Pengiriman'
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (isOnDuty && recipientName.trim().isNotBlank() && isSubmitEnabled) {
                        onSubmitProof(
                            recipientName.trim(),
                            selectedPhotoUri?.toString(),
                            codCashPhotoUri?.toString(),
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
                    imageVector = if (isOnDuty) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnDuty) "Submit PoD" else "Submit PoD · Terkunci (Status Off-Duty)",
                    fontSize = if (isOnDuty) 15.sp else 13.sp,
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
