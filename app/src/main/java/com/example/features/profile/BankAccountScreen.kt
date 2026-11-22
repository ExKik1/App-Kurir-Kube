package com.example.features.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.BankAccountChangeBottomSheet
import com.example.core.components.BankVerificationStatusPill
import com.example.core.components.DarkObsidianGlassCard
import com.example.core.components.KubeFeedbackManager
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassIconBox
import com.example.core.model.maskAccountNumber
import com.example.core.network.CourierSupabaseService
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

/**
 * Screen Rekening Bank Kurir.
 * - Menampilkan rekening utama dari `public.courier_bank_accounts` lengkap dengan status verifikasi.
 * - Tombol 'Ajukan Perubahan Rekening' membuka bottom sheet untuk mengirim data rekening baru + foto buku tabungan ke Supabase.
 */
@Composable
fun BankAccountScreen(
    repository: OrderRepository = OrderRepository.instance,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialCourier = remember(repository) { repository.courier.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }
    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val bankAccounts by supabaseService.bankAccounts.collectAsState()
    val primaryBank = remember(bankAccounts) {
        bankAccounts.firstOrNull { it.isPrimary } ?: bankAccounts.firstOrNull()
    }

    val displayBankName = primaryBank?.bankName?.takeIf { it.isNotBlank() && it != "-" }
        ?: courier.bankName.ifBlank { "Bank" }
    val displayAccountNumber = primaryBank?.accountNumber?.takeIf { it.isNotBlank() && it != "-" }
        ?: courier.bankAccountNumber
    val displayAccountHolder = primaryBank?.accountHolder?.takeIf { it.isNotBlank() && it != "-" }
        ?: courier.bankAccountHolder.ifBlank { courier.name.ifBlank { "-" } }

    var showBankChangeSheet by remember { mutableStateOf(false) }

    if (showBankChangeSheet) {
        BankAccountChangeBottomSheet(
            initialBankName = displayBankName,
            initialAccountHolder = displayAccountHolder,
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

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .testTag("bank_account_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 120.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KubeGlassCard(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_back_bank_account"),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = onBack
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = KubeTextDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Rekening Bank Kurir",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Text(
                            text = "Rekening pencairan dana komisi kurir",
                            fontSize = 12.sp,
                            color = Color(0xFF1E3A4C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Bank Card with Masked Number & Live Verification Badge
                DarkObsidianGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_rekening_utama"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalance,
                                        contentDescription = displayBankName,
                                        tint = Color(0xFF0D47A1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = displayBankName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (primaryBank?.isPrimary == true) "Rekening Utama" else "Rekening Pencairan",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.70f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            BankVerificationStatusPill(
                                verificationStatus = primaryBank?.verificationStatus
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "NOMOR REKENING (MASKED)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.65f),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Masked Account Number
                        Text(
                            text = if (displayAccountNumber.isBlank()) "Belum diatur" else maskAccountNumber(displayAccountNumber),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ATAS NAMA PEMILIK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.65f),
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = displayAccountHolder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Security Policy Note Card
                KubeGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        KubeGlassIconBox(
                            icon = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            boxSize = 38.dp,
                            iconSize = 18.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Kebijakan Keamanan Dana Kurir",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeTextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pengajuan rekening baru akan masuk ke daftar antrean verifikasi Admin sebelum dapat digunakan untuk penarikan saldo.",
                                fontSize = 12.sp,
                                color = Color(0xFF1E3A4C),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Single Button: Ajukan Perubahan Rekening
                Button(
                    onClick = { showBankChangeSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_ajukan_perubahan_rekening")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ajukan Perubahan Rekening",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
