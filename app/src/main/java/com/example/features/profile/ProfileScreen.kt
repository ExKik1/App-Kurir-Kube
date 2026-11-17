package com.example.features.profile

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.core.network.CourierSupabaseService
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.core.model.Courier
import com.example.core.model.maskAccountNumber
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

/**
 * Sub-screens navigable from Profile.
 */
enum class ProfileSubScreen {
    NONE,
    EDIT_PROFILE,
    DELIVERY_HISTORY,
    NOTIFICATIONS,
    BANK_ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: OrderRepository = OrderRepository.instance,
    onSignOut: () -> Unit = {}
) {
    var currentSubScreen by remember { mutableStateOf(ProfileSubScreen.NONE) }

    Crossfade(targetState = currentSubScreen, label = "profile_subscreen_crossfade") { subScreen ->
        when (subScreen) {
            ProfileSubScreen.EDIT_PROFILE -> {
                EditProfileScreen(
                    repository = repository,
                    onBack = { currentSubScreen = ProfileSubScreen.NONE }
                )
            }
            ProfileSubScreen.DELIVERY_HISTORY -> {
                DeliveryHistoryScreen(
                    repository = repository,
                    onBack = { currentSubScreen = ProfileSubScreen.NONE }
                )
            }
            ProfileSubScreen.NOTIFICATIONS -> {
                NotificationScreen(
                    onBack = { currentSubScreen = ProfileSubScreen.NONE }
                )
            }
            ProfileSubScreen.BANK_ACCOUNT -> {
                BankAccountScreen(
                    repository = repository,
                    onBack = { currentSubScreen = ProfileSubScreen.NONE }
                )
            }
            ProfileSubScreen.NONE -> {
                ProfileMainContent(
                    repository = repository,
                    onNavigateToSubScreen = { currentSubScreen = it },
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun ProfileMainContent(
    repository: OrderRepository,
    onNavigateToSubScreen: (ProfileSubScreen) -> Unit,
    onSignOut: () -> Unit
) {
    val supabaseService = remember(repository) { CourierSupabaseService.forRepository(repository) }
    val activeCourierProfileId by supabaseService.activeCourierProfileId.collectAsState()
    val initialCourier = remember(repository) { repository.courier.value }
    val courierProfileId = activeCourierProfileId.orEmpty().ifBlank { initialCourier.id }
    val courier by supabaseService.streamCourierProfile(courierProfileId).collectAsState(initial = initialCourier)
    val warehouse by supabaseService.streamWarehouseInfo().collectAsState(initial = com.example.core.network.WarehouseInfo())
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showVehicleInfoSheet by remember { mutableStateOf(false) }
    var showCredentialsSheet by remember { mutableStateOf(false) }
    var showWarehouseSheet by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    // 1. Support & Dispatcher Liquid Glass Dialog (Perbaikan Bantuan)
    if (showSupportDialog) {
        SupportGlassDialog(
            onDismiss = { showSupportDialog = false }
        )
    }

    // 5. Informasi Gudang / Pick Up Liquid Glass ModalBottomSheet
    if (showWarehouseSheet) {
        WarehousePickupBottomSheet(
            onDismiss = { showWarehouseSheet = false }
        )
    }

    // 3. Informasi Kendaraan ModalBottomSheet
    if (showVehicleInfoSheet) {
        VehicleInfoBottomSheet(
            courier = courier,
            onDismiss = { showVehicleInfoSheet = false }
        )
    }

    // 9. Info Kredensial & Biometrik ModalBottomSheet
    if (showCredentialsSheet) {
        CredentialsBiometricsBottomSheet(
            courier = courier,
            onToggleBiometric = { repository.updateBiometricEnabled(it) },
            onDismiss = { showCredentialsSheet = false }
        )
    }

    // 10. Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Konfirmasi Keluar",
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )
            },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin keluar dari akun kurir? Pastikan seluruh pesanan aktif telah diperbarui.",
                    color = KubeTextMuted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Ya, Keluar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    LiquidGlassBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("profile_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 120.dp)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(3.dp, CircleShape, ambientColor = Color(0x1A0083B0))
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)), CircleShape),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.45f)
                ) {
                    IconButton(
                        onClick = { showSupportDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HeadsetMic,
                            contentDescription = "Bantuan Dispatcher",
                            tint = Color(0xFF0F3657)
                        )
                    }
                }

                // Official Brand Ice Cube Logo in Profile Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_kube_cube),
                        contentDescription = "Logo Kubus Es Kube",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Profil Kurir Kube",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                }

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(3.dp, CircleShape, ambientColor = Color(0x1A0083B0))
                        .clip(CircleShape)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.85f)), CircleShape),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.45f)
                ) {
                    IconButton(
                        onClick = { onNavigateToSubScreen(ProfileSubScreen.EDIT_PROFILE) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Profil",
                            tint = Color(0xFF0F3657)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Courier Avatar & Details Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .shadow(8.dp, CircleShape, ambientColor = Color(0x330083B0))
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFFFFC107), CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.maryono_avatar),
                        contentDescription = "Avatar ${courier.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = courier.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = KubeTextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${courier.email} • ID: ${courier.id}",
                    fontSize = 13.sp,
                    color = KubeTextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Duty Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.50f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (courier.isActiveDuty) Color(0xFF00C853) else Color(0xFFB0BEC5))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (courier.isActiveDuty) "Status: Aktif Bertugas" else "Status: Sedang Istirahat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F3657)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ==========================================
            // STANDARISASI URUTAN MENU (HIERARKI OPERASIONAL 1-10)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Edit Profil -> EditProfileScreen
                ProfileMenuGlassItem(
                    icon = Icons.Filled.Person,
                    label = "Edit Profil",
                    subtitle = "Perbarui kontak, nomor HP, dan domisili",
                    tag = "profile_item_edit_profil",
                    onClick = { onNavigateToSubScreen(ProfileSubScreen.EDIT_PROFILE) }
                )

                // 2. Status Bertugas (Toggle Aktif/Tidak Aktif via CourierSupabaseService.toggleOnDuty)
                ProfileDutyToggleItem(
                    isActiveDuty = courier.isActiveDuty,
                    onToggle = { newDuty ->
                        coroutineScope.launch {
                            try {
                                CourierSupabaseService.instance.toggleOnDuty(
                                    courierId = courier.id,
                                    isOnDuty = newDuty
                                )
                            } catch (_: Exception) {
                                repository.updateCourierDuty(newDuty)
                            }
                        }
                    },
                    tag = "profile_item_status_bertugas"
                )

                // 3. Informasi Kendaraan (Menu Baru) -> ModalBottomSheet
                ProfileMenuGlassItem(
                    icon = Icons.Filled.LocalShipping,
                    label = "Informasi Kendaraan",
                    subtitle = "${courier.vehiclePlateNumber} • ${courier.vehicleCapacity}",
                    tag = "profile_item_informasi_kendaraan",
                    iconTint = KubeBlueBright,
                    badgeText = "Dinas Kube",
                    onClick = { showVehicleInfoSheet = true }
                )

                // 4. Riwayat Pengiriman -> DeliveryHistoryScreen (Selesai/Gagal)
                ProfileMenuGlassItem(
                    icon = Icons.Filled.History,
                    label = "Riwayat Pengiriman",
                    subtitle = "${courier.completedDeliveries} terkirim hari ini • 148 selesai bulan ini",
                    tag = "profile_item_riwayat_pengiriman",
                    onClick = { onNavigateToSubScreen(ProfileSubScreen.DELIVERY_HISTORY) }
                )

                // 5. Notifikasi -> NotificationScreen
                ProfileMenuGlassItem(
                    icon = Icons.Filled.Notifications,
                    label = "Notifikasi",
                    subtitle = "Order baru, rute prioritas, & peringatan sistem",
                    tag = "profile_item_notifikasi",
                    onClick = { onNavigateToSubScreen(ProfileSubScreen.NOTIFICATIONS) }
                )

                // 6. Informasi Gudang / Pick Up -> ModalBottomSheet Liquid Glass
                ProfileMenuGlassItem(
                    icon = Icons.Filled.Store,
                    label = "Informasi Gudang / Pick Up",
                    subtitle = warehouse.name,
                    tag = "profile_item_informasi_gudang_pick_up",
                    onClick = { showWarehouseSheet = true }
                )

                // 7. Rekening Bank -> BankAccountScreen (Masking BCA)
                ProfileMenuGlassItem(
                    icon = Icons.Filled.AccountBalance,
                    label = "Rekening Bank",
                    subtitle = "BCA • ${maskAccountNumber(courier.bankAccountNumber)} (a.n. ${courier.bankAccountHolder})",
                    tag = "profile_item_rekening_bank",
                    badgeText = "Terverifikasi",
                    onClick = { onNavigateToSubScreen(ProfileSubScreen.BANK_ACCOUNT) }
                )

                // 8. Bantuan / Hubungi Admin -> SupportGlassDialog (Dark Glass + Clickable)
                ProfileMenuGlassItem(
                    icon = Icons.Filled.HeadsetMic,
                    label = "Bantuan / Hubungi Admin",
                    subtitle = "Dispatcher Kube Call Center & WhatsApp Darurat",
                    tag = "profile_item_bantuan_hubungi_admin",
                    iconTint = Color(0xFF0284C7),
                    badgeText = "24 Jam",
                    onClick = { showSupportDialog = true }
                )

                // 9. Info Kredensial & Biometrik -> ModalBottomSheet
                ProfileMenuGlassItem(
                    icon = Icons.Filled.Fingerprint,
                    label = "Info Kredensial & Biometrik",
                    subtitle = "ID: ${courier.id} • Biometrik Aktif",
                    tag = "profile_item_info_kredensial_biometrik",
                    iconTint = Color(0xFF7C3AED),
                    badgeText = "Aman",
                    onClick = { showCredentialsSheet = true }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 10. Keluar / Logout
                ProfileMenuGlassItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Keluar / Logout",
                    subtitle = "Keluar dari sesi kurir saat ini",
                    tag = "profile_item_keluar_logout",
                    iconTint = Color(0xFFDC2626),
                    textColor = Color(0xFFDC2626),
                    onClick = { showLogoutConfirmDialog = true }
                )
            }
        }
    }
}

/**
 * Menu Item 2: Status Bertugas (Toggle Aktif/Tidak Aktif)
 */
@Composable
private fun ProfileDutyToggleItem(
    isActiveDuty: Boolean,
    onToggle: (Boolean) -> Unit,
    tag: String
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(22.dp),
        elevation = 4.dp,
        fillAlphaTop = 0.58f,
        fillAlphaBottom = 0.30f,
        borderAlphaTop = 0.95f,
        borderAlphaBottom = 0.38f,
        borderWidth = 1.2.dp,
        onClick = { onToggle(!isActiveDuty) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActiveDuty) Color(0xFFE8F5E9) else Color(0xFFECEFF1)
                        )
                        .border(
                            1.dp,
                            if (isActiveDuty) Color(0xFFC8E6C9) else Color(0xFFCFD8DC),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isActiveDuty) Icons.Filled.CheckCircle else Icons.Filled.ToggleOn,
                        contentDescription = "Status Bertugas",
                        tint = if (isActiveDuty) Color(0xFF2E7D32) else Color(0xFF78909C),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Status Bertugas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isActiveDuty) "Aktif Bertugas (Menerima Order Es)" else "Sedang Istirahat (Off-Duty)",
                        fontSize = 12.sp,
                        color = if (isActiveDuty) Color(0xFF2E7D32) else KubeTextMuted,
                        fontWeight = if (isActiveDuty) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            Switch(
                checked = isActiveDuty,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF00C853),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFB0BEC5)
                ),
                modifier = Modifier.testTag("switch_status_bertugas")
            )
        }
    }
}

/**
 * BottomSheet for Menu 3: Informasi Kendaraan (Read-Only)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleInfoBottomSheet(
    courier: Courier,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("sheet_informasi_kendaraan")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Informasi Kendaraan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "Data armada dinas kurir Kube (Read-Only)",
                        fontSize = 12.sp,
                        color = KubeTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            VehicleDetailCard(
                icon = Icons.Filled.LocalShipping,
                label = "Jenis Kendaraan",
                value = courier.vehicleType,
                extraInfo = "Box Berisolasi Pendingin • Suhu Standar -18°C"
            )

            VehicleDetailCard(
                icon = Icons.Filled.Badge,
                label = "Nomor Polisi (Plat)",
                value = courier.vehiclePlateNumber,
                extraInfo = "Terdaftar di Samsat Banyumas • KIR & Pajak Aktif",
                isHighlight = true
            )

            VehicleDetailCard(
                icon = Icons.Filled.Inbox,
                label = "Kapasitas Angkut",
                value = courier.vehicleCapacity,
                extraInfo = "Daya tampung optimal es balok 25kg & tube kristal"
            )

            VehicleDetailCard(
                icon = Icons.Filled.CheckCircle,
                label = "Status Kendaraan",
                value = courier.vehicleStatus,
                extraInfo = "Servis Terakhir: 15 Okt 2026 • Mesin & Kompresor Prima"
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Data kendaraan terdaftar resmi dalam inventaris Kube. Hubungi koordinator logistik jika ada kendala mesin atau suhu box.",
                        fontSize = 11.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_close_vehicle_sheet")
            ) {
                Text("Tutup", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun VehicleDetailCard(
    icon: ImageVector,
    label: String,
    value: String,
    extraInfo: String,
    isHighlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isHighlight) KubeBlueBright.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
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
                    .background(if (isHighlight) Color(0xFFE0F2FE) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) KubeBlueBright else Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 11.sp, color = KubeTextMuted)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KubeTextDark)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = extraInfo, fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
    }
}

/**
 * BottomSheet for Menu 9: Info Kredensial & Biometrik
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredentialsBiometricsBottomSheet(
    courier: Courier,
    onToggleBiometric: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("sheet_kredensial_biometrik")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE9FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Info Kredensial & Biometrik",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "Pengaturan keamanan & otentikasi kurir",
                        fontSize = 12.sp,
                        color = KubeTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ID Kurir", fontSize = 11.sp, color = KubeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = courier.id,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                        Text(
                            text = "Read-Only",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Email Terdaftar", fontSize = 11.sp, color = KubeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = courier.email,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                        Text(
                            text = "Read-Only",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aktifkan Login Sidik Jari / Face ID",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KubeTextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Masuk instan menggunakan biometrik perangkat",
                                fontSize = 11.sp,
                                color = KubeTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = courier.isBiometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF7C3AED)
                        ),
                        modifier = Modifier.testTag("switch_biometric_login")
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notice_password_managed_by_system")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                            contentDescription = "Terkunci",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Kata sandi dikelola oleh sistem. Hubungi Admin untuk perubahan.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kurir tidak diizinkan mengubah kata sandi secara mandiri demi kepatuhan keamanan data operasional Kube.",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_close_credentials_sheet")
            ) {
                Text("Selesai", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

/**
 * Standardized profile menu item with liquid glass styling, optional badge, subtitle, and tag.
 */
@Composable
private fun ProfileMenuGlassItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    badgeText: String? = null,
    tag: String,
    iconTint: Color = Color(0xFF0F3657),
    textColor: Color = KubeTextDark,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(22.dp),
        elevation = 3.dp,
        fillAlphaTop = 0.55f,
        fillAlphaBottom = 0.28f,
        borderAlphaTop = 0.92f,
        borderAlphaBottom = 0.35f,
        borderWidth = 1.2.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                        if (badgeText != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = KubeGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, KubeGreen.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = KubeTextMuted
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (textColor == Color(0xFFDC2626)) Color(0xFFDC2626) else KubeBlueBright,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
