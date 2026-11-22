package com.example.core.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.network.CourierBankAccountRecord
import com.example.core.network.CourierSupabaseService
import com.example.ui.theme.KubeBlueBright
import kotlinx.coroutines.launch

/**
 * Lencana status verifikasi rekening kurir dari tabel `public.courier_bank_accounts`.
 */
@Composable
fun BankVerificationStatusPill(
    verificationStatus: String?,
    modifier: Modifier = Modifier
) {
    val normalized = verificationStatus.orEmpty().trim().lowercase()
    val (label, textColor) = when {
        normalized == "verified" || normalized == "approved" ->
            "Terverifikasi" to Color(0xFF059669)
        normalized == "rejected" ->
            "Ditolak" to Color(0xFFDC2626)
        else ->
            "Menunggu Verifikasi Admin" to Color(0xFFD97706)
    }

    KubeGlassPill(
        text = label,
        textColor = textColor,
        modifier = modifier
    )
}

/**
 * ModalBottomSheet untuk mengajukan perubahan atau pendaftaran rekening bank baru
 * ke tabel `public.courier_bank_accounts` lengkap dengan unggah foto buku tabungan opsional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountChangeBottomSheet(
    initialBankName: String = "",
    initialAccountHolder: String = "",
    onDismiss: () -> Unit,
    onSubmitSuccess: (CourierBankAccountRecord) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var bankName by remember { mutableStateOf(initialBankName.takeIf { it != "-" }.orEmpty()) }
    var accountNumber by remember { mutableStateOf("") }
    var accountHolder by remember { mutableStateOf(initialAccountHolder.takeIf { it != "-" }.orEmpty()) }
    var passbookPhotoUri by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val passbookPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            passbookPhotoUri = uri.toString()
            errorMessage = null
        }
    }

    val canSubmit = !isSubmitting &&
        bankName.isNotBlank() &&
        accountNumber.isNotBlank() &&
        accountHolder.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier
            .imePadding()
            .testTag("sheet_ajukan_perubahan_rekening")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    DbTitleText(text = "Ajukan Perubahan Rekening")
                    Spacer(modifier = Modifier.height(2.dp))
                    DbBodyText(
                        text = "Rekening baru akan diverifikasi Admin sebelum pencairan aktif",
                        maxLines = 2,
                        color = KubeMutedText
                    )
                }
                IconButton(
                    onClick = { if (!isSubmitting) onDismiss() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = KubeMutedText
                    )
                }
            }

            // Input Nama Bank
            OutlinedTextField(
                value = bankName,
                onValueChange = {
                    if (!isSubmitting) {
                        bankName = it
                        errorMessage = null
                    }
                },
                enabled = !isSubmitting,
                label = { Text("Nama Bank") },
                placeholder = { Text("Contoh: BCA, BRI, Mandiri, BNI") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                shape = RoundedCornerShape(14.dp),
                colors = kubeLightTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_bank_name")
            )

            // Input Nomor Rekening
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { input ->
                    if (!isSubmitting) {
                        accountNumber = input.filter { it.isDigit() }
                        errorMessage = null
                    }
                },
                enabled = !isSubmitting,
                label = { Text("Nomor Rekening") },
                placeholder = { Text("Masukkan nomor rekening baru") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                shape = RoundedCornerShape(14.dp),
                colors = kubeLightTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_number")
            )

            // Input Atas Nama Pemilik
            OutlinedTextField(
                value = accountHolder,
                onValueChange = {
                    if (!isSubmitting) {
                        accountHolder = it
                        errorMessage = null
                    }
                },
                enabled = !isSubmitting,
                label = { Text("Atas Nama Pemilik Rekening") },
                placeholder = { Text("Sesuai nama di buku tabungan") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                shape = RoundedCornerShape(14.dp),
                colors = kubeLightTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_account_holder")
            )

            // Input Opsional Foto Buku Tabungan (`passbookPhotoUri`)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            DbTitleText(text = "Foto Buku Tabungan (Opsional)")
                            Spacer(modifier = Modifier.height(2.dp))
                            DbBodyText(
                                text = "Lampirkan halaman depan buku tabungan untuk mempercepat verifikasi Admin",
                                maxLines = 2,
                                color = KubeMutedText
                            )
                        }
                    }

                    if (!passbookPhotoUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = passbookPhotoUri,
                                contentDescription = "Pratinjau foto buku tabungan",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF059669),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Foto Dipilih",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clickable(enabled = !isSubmitting) {
                                        passbookPhotoUri = null
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Hapus foto",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            passbookPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_pick_passbook_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (passbookPhotoUri == null) "Pilih Foto Buku Tabungan" else "Ganti Foto Buku Tabungan",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            // Info Notice
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
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
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DbBodyText(
                        text = "Rekening baru akan langsung tercatat dengan status Menunggu Verifikasi Admin.",
                        maxLines = 2,
                        color = Color(0xFF92400E)
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFDC2626)
                )
            }

            // Submit Button ("Ajukan")
            Button(
                onClick = {
                    if (!canSubmit) return@Button
                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        val res = CourierSupabaseService.instance.submitBankAccountRequest(
                            bankName = bankName,
                            accountNumber = accountNumber,
                            accountHolder = accountHolder,
                            passbookPhotoUri = passbookPhotoUri,
                            context = context
                        )
                        isSubmitting = false
                        res.fold(
                            onSuccess = { created ->
                                onSubmitSuccess(created)
                            },
                            onFailure = { err ->
                                errorMessage = err.message ?: "Gagal mengirim pengajuan rekening."
                            }
                        )
                    }
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("btn_submit_bank_change")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.4.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mengirim ke Database...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ajukan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
