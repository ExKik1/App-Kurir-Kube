package com.example.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.DeliveryStatus
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeGreen
import com.example.ui.theme.KubeOrange
import com.example.ui.theme.KubeRed

@Composable
fun StatusBadge(
    status: DeliveryStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        DeliveryStatus.NEW -> Pair(Color(0xFFE1F5FE), Color(0xFF0288D1))
        DeliveryStatus.IN_TRANSIT -> Pair(Color(0xFFE0F7FA), Color(0xFF0083B0))
        DeliveryStatus.ARRIVED -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        DeliveryStatus.PENDING_VERIFICATION -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        DeliveryStatus.DELIVERED -> Pair(Color(0xFFE8F8F5), Color(0xFF00C853))
        DeliveryStatus.FAILED -> Pair(Color(0xFFFFEBEE), KubeRed)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
