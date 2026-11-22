package com.example.features.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.components.KubeFeedbackManager
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassPill
import com.example.core.components.kubeLightTextFieldColors
import com.example.core.network.CourierSupabaseService
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

/**
 * Screen untuk Edit Profil Kurir.
 * - Form mengedit 'Nomor HP' dan 'Alamat Domisili'.
 * - Kolom 'Nama Lengkap' dan 'Email' adalah READ-ONLY (tidak bisa diedit).
 * - Dibungkus dengan SafeArea / statusBarsPadding.
 */
@Composable
fun EditProfileScreen(
    repository: OrderRepository = OrderRepository.instance,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialCourier = remember(repository) { repository.courier.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }
    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var phoneInput by remember(courier.phone) { mutableStateOf(courier.phone) }
    var addressInput by remember(courier.address) { mutableStateOf(courier.address) }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .testTag("edit_profile_screen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 120.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KubeGlassCard(
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_back_edit_profile"),
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

                    Text(
                        text = "Edit Profil Kurir",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                }

                // Avatar Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFFFFC107), CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.maryono_avatar),
                            contentDescription = "Avatar Kurir",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = courier.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "ID: ${courier.id} • Wilayah Banyumas",
                        fontSize = 12.sp,
                        color = KubeTextMuted
                    )
                }

                // Form Container
                KubeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Kolom Nama Lengkap (READ-ONLY)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Nama Lengkap",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KubeTextDark
                                )
                                KubeGlassPill(
                                    text = "Read-Only",
                                    textColor = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = courier.name,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Terkunci",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                                shape = RoundedCornerShape(14.dp),
                                colors = kubeLightTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("field_nama_readonly")
                            )
                        }

                        // 2. Kolom Email (READ-ONLY)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Email Terdaftar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KubeTextDark
                                )
                                KubeGlassPill(
                                    text = "Read-Only",
                                    textColor = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = courier.email,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Terkunci",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                                shape = RoundedCornerShape(14.dp),
                                colors = kubeLightTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("field_email_readonly")
                            )
                        }

                        // 3. Kolom Nomor HP (BISA DIEDIT)
                        Column {
                            Text(
                                text = "Nomor Handphone (WhatsApp)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubeTextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = KubeBlueBright
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                                shape = RoundedCornerShape(14.dp),
                                colors = kubeLightTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("field_nomor_hp")
                            )
                        }

                        // 4. Kolom Alamat Domisili (BISA DIEDIT)
                        Column {
                            Text(
                                text = "Alamat Domisili Kurir",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KubeTextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = null,
                                        tint = KubeBlueBright
                                    )
                                },
                                minLines = 2,
                                maxLines = 3,
                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                                shape = RoundedCornerShape(14.dp),
                                colors = kubeLightTextFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("field_alamat_domisili")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tombol Simpan Perubahan
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val res = supabaseService.updateCourierProfile(
                                        courierId = courier.id,
                                        phone = phoneInput,
                                        address = addressInput
                                    )
                                    res.fold(
                                        onSuccess = {
                                            KubeFeedbackManager.showSuccess(
                                                "Profil Diperbarui",
                                                "Nomor WhatsApp dan alamat domisili kurir berhasil disimpan."
                                            )
                                        },
                                        onFailure = { err ->
                                            KubeFeedbackManager.showError(
                                                "Gagal Memperbarui Profil",
                                                err.message ?: "Terjadi kendala saat menyimpan perubahan profil."
                                            )
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_simpan_profil")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simpan Perubahan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
