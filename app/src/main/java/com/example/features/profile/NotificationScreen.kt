package com.example.features.profile

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.DbBodyText
import com.example.core.components.DbTitleText
import com.example.core.components.KubeAtmosphericBackground
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassIconBox
import com.example.core.components.KubeGlassPill
import com.example.core.components.KubeMutedText
import com.example.core.components.KubePrimaryText
import com.example.core.network.CourierSupabaseService
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted
import kotlinx.coroutines.launch

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
 * Screen untuk Notifikasi Pemberitahuan Sistem Kurir (Real-Time Stream).
 * Mengalirkan pesan sistem dari CourierSupabaseService.streamNotifications dan menyinkronkan status dibaca (`is_read`).
 */
@Composable
fun NotificationScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val coroutineScope = rememberCoroutineScope()
    val activeCourierProfileId by CourierSupabaseService.instance.activeCourierProfileId.collectAsState()
    val notificationStream by CourierSupabaseService.instance
        .streamNotifications(activeCourierProfileId.orEmpty())
        .collectAsState(initial = emptyList())

    val notifications = remember(notificationStream) {
        notificationStream.map { record ->
            val (icon, tint, bg) = when (record.category) {
                "order" -> Triple(Icons.Filled.LocalShipping, KubeBlueBright, Color(0xFFE0F2FE))
                "wallet" -> Triple(Icons.Filled.Payment, KubeGreen, Color(0xFFE8F5E9))
                "pod" -> Triple(Icons.Filled.CheckCircle, Color(0xFF00897B), Color(0xFFE0F2F1))
                "warehouse" -> Triple(Icons.Filled.Campaign, Color(0xFFE65100), Color(0xFFFFF3E0))
                else -> Triple(Icons.Filled.CloudQueue, Color(0xFF1E88E5), Color(0xFFE3F2FD))
            }
            SystemNotification(
                id = record.id,
                title = record.title,
                message = record.message,
                time = record.time,
                icon = icon,
                iconTint = tint,
                iconBg = bg,
                isUnread = record.isUnread
            )
        }
    }

    val hasUnread = remember(notifications) { notifications.any { it.isUnread } }

    KubeAtmosphericBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("notification_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KubeGlassCard(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_back_notification"),
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifikasi Sistem",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = KubeTextDark
                    )
                    Text(
                        text = "Pemberitahuan tugas, keuangan, dan sistem Kube",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A4C)
                    )
                }

                if (hasUnread) {
                    TextButton(
                        onClick = {
                            val unreadItems = notifications.filter { it.isUnread }
                            coroutineScope.launch {
                                unreadItems.forEach { item ->
                                    CourierSupabaseService.instance.markNotificationAsRead(item.id)
                                }
                            }
                        },
                        modifier = Modifier.testTag("btn_mark_all_notifications_read")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = KubeBlueBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tandai semua dibaca",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeBlueBright
                        )
                    }
                }
            }

            if (notifications.isEmpty()) {
                KubeGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("empty_notifications_card"),
                    shape = RoundedCornerShape(22.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KubeGlassIconBox(
                            icon = Icons.Filled.NotificationsNone,
                            contentDescription = null,
                            tint = KubeMutedText,
                            boxSize = 48.dp,
                            iconSize = 24.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DbTitleText(text = "Belum Ada Notifikasi")
                        Spacer(modifier = Modifier.height(4.dp))
                        DbBodyText(
                            text = "Pemberitahuan tugas pengiriman, keuangan, dan verifikasi rekening akan muncul di sini.",
                            maxLines = 2,
                            color = Color(0xFF1E3A4C)
                        )
                    }
                }
            } else {
                // List of Notifications
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list_notifications"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        NotificationGlassCard(
                            item = notif,
                            onClick = {
                                if (notif.isUnread) {
                                    coroutineScope.launch {
                                        CourierSupabaseService.instance.markNotificationAsRead(notif.id)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationGlassCard(
    item: SystemNotification,
    onClick: () -> Unit
) {
    KubeGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notif_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        isHighlighted = item.isUnread,
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            KubeGlassIconBox(
                icon = item.icon,
                contentDescription = null,
                tint = item.iconTint,
                isHighlighted = item.isUnread,
                boxSize = 44.dp,
                iconSize = 22.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DbTitleText(
                        text = item.title,
                        color = KubePrimaryText,
                        modifier = Modifier.weight(1f)
                    )

                    if (item.isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        KubeGlassPill(
                            text = "Baru",
                            textColor = Color(0xFF0284C7)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                DbBodyText(
                    text = item.message,
                    maxLines = 4,
                    color = Color(0xFF1E3A4C)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF1E3A4C),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.time,
                        fontSize = 11.sp,
                        color = Color(0xFF1E3A4C)
                    )
                }
            }
        }
    }
}
