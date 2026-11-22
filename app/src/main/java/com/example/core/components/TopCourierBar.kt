package com.example.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.network.CourierSupabaseService
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark

@Composable
fun TopCourierBar(
    name: String = "Kurir Kube",
    statusSubtitle: String = "Selamat Bertugas",
    periodBadgeText: String? = "Hari Ini",
    isOnDuty: Boolean = true,
    showNotifButton: Boolean = true,
    textColor: Color = KubeTextDark,
    rightActionContent: (@Composable () -> Unit)? = null,
    onAvatarClick: () -> Unit = {},
    onToggleDutyClick: (() -> Unit)? = null,
    onNotifClick: () -> Unit = {}
) {
    val notifications by CourierSupabaseService.instance.notifications.collectAsState()
    val unreadCount = remember(notifications) { notifications.count { it.isUnread } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Courier Avatar with circular yellow border and shadow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFFFC107), CircleShape)
                    .clickable(onClick = onAvatarClick)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.maryono_avatar),
                    contentDescription = "Foto Profil $name",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                DbTitleText(
                    text = name,
                    color = textColor,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClick = onAvatarClick)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Duty Status Pill (tappable when onToggleDutyClick is provided) and Period Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pillBg = if (isOnDuty) {
                        Color.White.copy(alpha = 0.72f)
                    } else {
                        Color(0xFFFEF3C7)
                    }
                    val pillBorder = if (isOnDuty) {
                        Color.White.copy(alpha = 0.90f)
                    } else {
                        Color(0xFFF59E0B).copy(alpha = 0.75f)
                    }
                    val pillModifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(pillBg)
                        .border(BorderStroke(0.9.dp, pillBorder), RoundedCornerShape(50))
                        .let { base ->
                            if (onToggleDutyClick != null) {
                                base
                                    .heightIn(min = 32.dp)
                                    .clickable(onClick = onToggleDutyClick)
                            } else {
                                base
                            }
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("badge_toggle_duty")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = pillModifier
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnDuty) KubeGreen else Color(0xFFD97706))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusSubtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOnDuty) KubePrimaryText else Color(0xFF92400E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (onToggleDutyClick != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.PowerSettingsNew,
                                contentDescription = if (isOnDuty) "Nonaktifkan On-Duty" else "Aktifkan On-Duty",
                                tint = if (isOnDuty) Color(0xFF059669) else Color(0xFFD97706),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (periodBadgeText != null) {
                        DbStatusPill(
                            text = periodBadgeText,
                            backgroundColor = Color(0xFF00B4DB).copy(alpha = 0.16f),
                            textColor = Color(0xFF007299),
                            modifier = Modifier.testTag("courier_bar_period_badge")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (rightActionContent != null) {
            rightActionContent()
        } else if (showNotifButton) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = onNotifClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("notification_button"),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifikasi Tugas",
                            tint = Color(0xFF0F3657),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .border(1.5.dp, Color.White, CircleShape)
                            .padding(horizontal = 4.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("notification_unread_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp
                        )
                    }
                }
            }
        }
    }
}
