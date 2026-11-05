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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.LiquidDarkGlassCard
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.core.model.maskAccountNumber
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

/**
 * Screen Rekening Bank Kurir.
 * - Card rekening BCA dengan string masking (contoh: ******2831 atau **** **** 2831).
 * - Tombol 'Ajukan Perubahan Rekening' yang HANYA memunculkan snackbar atau dialog:
 *   'Permintaan perubahan rekening telah dikirim ke Admin. Menunggu verifikasi'.
 */
@Composable
fun BankAccountScreen(
    repository: OrderRepository = OrderRepository.instance,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val courier by repository.courier.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showConfirmationDialog by remember { mutableStateOf(false) }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Permintaan Terkirim",
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
            },
            text = {
                Text(
                    text = "Permintaan perubahan rekening telah dikirim ke Admin. Menunggu verifikasi.",
                    fontSize = 14.sp,
                    color = KubeTextDark,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirmationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LiquidGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .testTag("bank_account_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(3.dp, CircleShape, ambientColor = Color(0x1A0083B0))
                            .clip(CircleShape)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)), CircleShape),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.50f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("btn_back_bank_account")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = KubeTextDark
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
                            color = KubeTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BCA Card with Masked Number
                LiquidDarkGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_rekening_utama"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalance,
                                        contentDescription = "BCA",
                                        tint = Color(0xFF0D47A1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = courier.bankName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Bank Central Asia",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.70f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = KubeGreen.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, KubeGreen.copy(alpha = 0.50f))
                            ) {
                                Text(
                                    text = "Utama & Terverifikasi",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KubeCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
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
                            text = maskAccountNumber(courier.bankAccountNumber),
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
                            text = courier.bankAccountHolder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Security Policy Note Card
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 2.dp,
                    fillAlphaTop = 0.50f,
                    fillAlphaBottom = 0.25f
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                        }

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
                                text = "Demi keamanan penghasilan Anda dan menghindari penyalahgunaan akun, kurir tidak dapat mengubah nomor rekening secara mandiri.",
                                fontSize = 12.sp,
                                color = KubeTextMuted,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Single Button: Ajukan Perubahan Rekening
                Button(
                    onClick = {
                        showConfirmationDialog = true
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "Permintaan perubahan rekening telah dikirim ke Admin. Menunggu verifikasi."
                            )
                        }
                    },
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
