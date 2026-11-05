package com.example.features.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.components.LiquidDarkGlassCard
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import com.example.ui.theme.KubeGreen

/**
 * Dialog Bantuan & Dispatcher Kube dengan tema Liquid Dark Glass.
 * Teks berwarna putih/terang dengan kontras tinggi, dan nomor kontak/WhatsApp
 * dapat diklik langsung untuk membuka aplikasi telepon atau WhatsApp.
 */
@Composable
fun SupportGlassDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        LiquidDarkGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("dialog_bantuan_admin"),
            shape = RoundedCornerShape(26.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                // Header with icon and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(KubeBlueBright.copy(alpha = 0.25f))
                                .border(1.dp, KubeCyan.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HeadsetMic,
                                contentDescription = null,
                                tint = KubeCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Bantuan & Dispatcher",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Pusat Koordinasi Lapangan Kube 24 Jam",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Info Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Klik pada kontak di bawah untuk langsung menghubungi dispatcher logistik Kube saat terjadi kendala rute, stok, atau kendaraan.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.90f),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Contact Actions List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. WhatsApp Dispatcher
                    SupportActionGlassItem(
                        icon = Icons.Filled.Message,
                        iconTint = KubeGreen,
                        iconBg = KubeGreen.copy(alpha = 0.20f),
                        title = "WhatsApp Dispatcher",
                        subtitle = "0811-2345-6789 (Respon Cepat)",
                        tag = "btn_contact_whatsapp",
                        onClick = {
                            openWhatsApp(context, "6281123456789", "Halo Dispatcher Kube, saya kurir Maryono Supardi (CR-098) membutuhkan bantuan operasional.")
                        }
                    )

                    // 2. Hotline Telepon Kantor
                    SupportActionGlassItem(
                        icon = Icons.Filled.Phone,
                        iconTint = KubeCyan,
                        iconBg = KubeCyan.copy(alpha = 0.20f),
                        title = "Hotline Telepon Kantor",
                        subtitle = "(0281) 632-889 (Pabrik Es Banyumas)",
                        tag = "btn_contact_phone",
                        onClick = {
                            dialPhoneNumber(context, "0281632889")
                        }
                    )

                    // 3. Layanan Darurat Kendaraan
                    SupportActionGlassItem(
                        icon = Icons.Filled.Warning,
                        iconTint = Color(0xFFFFB74D),
                        iconBg = Color(0xFFFFB74D).copy(alpha = 0.20f),
                        title = "Darurat Armada & Mesin",
                        subtitle = "0812-9988-7766 (Derek / Mekanik)",
                        tag = "btn_contact_emergency",
                        onClick = {
                            dialPhoneNumber(context, "081299887766")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_close_support_dialog")
                ) {
                    Text(
                        text = "Tutup",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportActionGlassItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.80f)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = KubeCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun dialPhoneNumber(context: Context, phoneNumber: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Membuka telepon: $phoneNumber", Toast.LENGTH_SHORT).show()
    }
}

private fun openWhatsApp(context: Context, phoneNumber: String, message: String) {
    runCatching {
        val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Membuka WhatsApp: +$phoneNumber", Toast.LENGTH_SHORT).show()
    }
}
