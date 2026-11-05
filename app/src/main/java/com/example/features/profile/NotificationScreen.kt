package com.example.features.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.LiquidGlassBackground
import com.example.core.components.LiquidGlassCard
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

data class SystemNotification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val isUnread: Boolean = false
)

/**
 * Screen untuk Notifikasi Pemberitahuan Sistem Kurir.
 * Menampilkan pesan sistem: Tugas Baru Masuk, Pencairan Berhasil, PoD Disetujui, dll.
 */
@Composable
fun NotificationScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val notifications by remember {
        mutableStateOf(
            listOf(
                SystemNotification(
                    id = "NOTIF-01",
                    title = "Tugas Baru Masuk",
                    message = "Pesanan #ESB-00128 (10 balok es) ke Toko Es Segar telah ditugaskan ke armada Anda.",
                    time = "10 menit yang lalu",
                    icon = Icons.Filled.LocalShipping,
                    iconTint = KubeBlueBright,
                    iconBg = Color(0xFFE0F2FE),
                    isUnread = true
                ),
                SystemNotification(
                    id = "NOTIF-02",
                    title = "Pencairan Berhasil",
                    message = "Pencairan saldo komisi Rp 1.200.000 ke rekening BCA ****2831 telah sukses diproses finance Kube.",
                    time = "1 jam yang lalu",
                    icon = Icons.Filled.Payment,
                    iconTint = KubeGreen,
                    iconBg = Color(0xFFE8F5E9),
                    isUnread = false
                ),
                SystemNotification(
                    id = "NOTIF-03",
                    title = "Verifikasi PoD Disetujui",
                    message = "Laporan bukti pengantaran order #ESB-00126 (Supermarket Sehat) telah lolos verifikasi Admin.",
                    time = "3 jam yang lalu",
                    icon = Icons.Filled.CheckCircle,
                    iconTint = Color(0xFF00897B),
                    iconBg = Color(0xFFE0F2F1),
                    isUnread = false
                ),
                SystemNotification(
                    id = "NOTIF-04",
                    title = "Pengumuman Operasional Gudang",
                    message = "Pabrik Es Kube Banyumas beroperasi normal 24 jam dengan fasilitas loading crane es balok cepat.",
                    time = "Hari ini, 06:00",
                    icon = Icons.Filled.Campaign,
                    iconTint = Color(0xFFE65100),
                    iconBg = Color(0xFFFFF3E0),
                    isUnread = false
                ),
                SystemNotification(
                    id = "NOTIF-05",
                    title = "Peringatan Rute & Cuaca",
                    message = "Hujan gerimis terpantau di jalur Purwokerto Barat. Harap pastikan pintu freezer box terkunci rapat.",
                    time = "Kemarin, 16:30",
                    icon = Icons.Filled.CloudQueue,
                    iconTint = Color(0xFF1E88E5),
                    iconBg = Color(0xFFE3F2FD),
                    isUnread = false
                )
            )
        )
    }

    LiquidGlassBackground(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("notification_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        modifier = Modifier.testTag("btn_back_notification")
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
                        text = "Notifikasi Sistem",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "Pemberitahuan tugas, keuangan, dan sistem Kube",
                        fontSize = 12.sp,
                        color = KubeTextMuted
                    )
                }
            }

            // List of Notifications
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("list_notifications"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    NotificationGlassCard(item = notif)
                }
            }
        }
    }
}

@Composable
private fun NotificationGlassCard(item: SystemNotification) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        elevation = 3.dp,
        fillAlphaTop = if (item.isUnread) 0.65f else 0.50f,
        fillAlphaBottom = if (item.isUnread) 0.35f else 0.25f,
        borderAlphaTop = 0.90f,
        borderAlphaBottom = 0.35f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(item.iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )

                    if (item.isUnread) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = KubeBlueBright
                        ) {
                            Text(
                                text = "Baru",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.message,
                    fontSize = 13.sp,
                    color = KubeTextDark.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = KubeTextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.time,
                        fontSize = 11.sp,
                        color = KubeTextMuted
                    )
                }
            }
        }
    }
}
