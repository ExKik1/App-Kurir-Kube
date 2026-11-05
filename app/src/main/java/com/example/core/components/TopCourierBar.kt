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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeTextDark

@Composable
fun TopCourierBar(
    name: String = "Maryono Supardi",
    statusSubtitle: String = "Selamat Bertugas",
    periodBadgeText: String? = "Hari Ini",
    isOnDuty: Boolean = true,
    showSearchAndNotif: Boolean = true,
    textColor: Color = KubeTextDark,
    rightActionContent: (@Composable () -> Unit)? = null,
    onAvatarClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotifClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onAvatarClick)
        ) {
            // Courier Avatar with circular yellow border and shadow
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFFFC107), CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.maryono_avatar),
                    contentDescription = "Foto Kurir Maryono Supardi",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Duty Status Pill and Hari Ini Period Tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.55f))
                            .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.90f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnDuty) KubeGreen else Color.LightGray)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusSubtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F3657)
                        )
                    }

                    if (periodBadgeText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF00B4DB).copy(alpha = 0.15f))
                                .border(BorderStroke(0.8.dp, Color(0xFF00B4DB).copy(alpha = 0.40f)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                .testTag("courier_bar_period_badge")
                        ) {
                            Text(
                                text = periodBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007299)
                            )
                        }
                    }
                }
            }
        }

        if (rightActionContent != null) {
            rightActionContent()
        } else if (showSearchAndNotif) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Glass Search button
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
                        onClick = onSearchClick,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Cari Pesanan",
                            tint = Color(0xFF0F3657),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Glass Notification button
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
                        onClick = onNotifClick,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifikasi Tugas",
                            tint = Color(0xFF0F3657),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
