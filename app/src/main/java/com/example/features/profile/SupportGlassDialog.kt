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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.example.core.components.DarkObsidianGlassCard
import com.example.core.components.InAppChatBottomSheet
import com.example.core.network.CourierSupabaseService
import com.example.core.network.SupportContactRecord
import com.example.core.repository.OrderRepository
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import com.example.ui.theme.KubeGreen

/**
 * Dialog Bantuan & Admin Kube dengan tema Liquid Dark Glass.
 * Menampilkan daftar kontak bantuan secara real-time dari tabel `support_contacts` Supabase
 * serta aksi langsung untuk menelepon atau mengirim pesan WhatsApp.
 */
@Composable
fun SupportGlassDialog(
    onOpenAdminLiveChat: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val courier by OrderRepository.instance.courier.collectAsState()
    val supportContacts by CourierSupabaseService.instance.supportContacts.collectAsState()
    var showInternalAdminChat by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { CourierSupabaseService.instance.syncAllTablesOnce() }
    }

    if (showInternalAdminChat) {
        InAppChatBottomSheet(
            title = "Admin Operasional Kube",
            subtitle = "Live Chat Operasional & Dispatcher",
            roomType = "courier_admin",
            onDismiss = { showInternalAdminChat = false }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        DarkObsidianGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("dialog_bantuan_admin"),
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
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
                                text = "Bantuan & Admin Kube",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Kontak Operasional & Dispatcher Resmi",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pilih kontak admin atau dispatcher di bawah untuk menghubungi via WhatsApp atau panggilan telepon langsung.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.90f),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary CTA: Live Chat Admin di Aplikasi
                Button(
                    onClick = {
                        if (onOpenAdminLiveChat != null) {
                            onOpenAdminLiveChat()
                        } else {
                            showInternalAdminChat = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0096C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("btn_live_chat_admin")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Live Chat Admin",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Chat Admin di Aplikasi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Contact Actions List (Live from Supabase `support_contacts`)
                if (supportContacts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("support_contacts_empty")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SupportAgent,
                                contentDescription = null,
                                tint = KubeCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Memuat daftar kontak bantuan dari Supabase... Pastikan koneksi internet tersedia.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.80f),
                                lineHeight = 17.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        supportContacts.forEach { contact ->
                            SupportContactGlassCard(
                                contact = contact,
                                courierName = courier.name,
                                courierId = courier.id.ifBlank { "-" },
                                context = context
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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
private fun SupportContactGlassCard(
    contact: SupportContactRecord,
    courierName: String,
    courierId: String,
    context: Context
) {
    val serviceLower = contact.serviceType.lowercase()
    val (icon: ImageVector, iconTint: Color, iconBg: Color) = when {
        serviceLower.contains("darurat") || serviceLower.contains("mekanik") || serviceLower.contains("armada") -> {
            Triple(Icons.Filled.Warning, Color(0xFFFFB74D), Color(0xFFFFB74D).copy(alpha = 0.20f))
        }
        serviceLower.contains("wa") || serviceLower.contains("whatsapp") || serviceLower.contains("dispatcher") -> {
            Triple(Icons.Filled.Message, KubeGreen, KubeGreen.copy(alpha = 0.20f))
        }
        else -> {
            Triple(Icons.Filled.Phone, KubeCyan, KubeCyan.copy(alpha = 0.20f))
        }
    }

    val waDigits = normalizeWhatsappDigits(contact.phoneNumber)
    val greetingMsg = "Halo ${contact.contactName}, saya kurir $courierName (ID: $courierId) membutuhkan bantuan (${contact.serviceType})."

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (serviceLower.contains("wa") || serviceLower.contains("whatsapp")) {
                    openWhatsApp(context, waDigits, greetingMsg)
                } else {
                    dialPhoneNumber(context, contact.phoneNumber)
                }
            }
            .testTag("support_contact_item_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = contact.serviceType,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.contactName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${contact.serviceType} • ${contact.serviceHours}",
                        fontSize = 11.sp,
                        color = KubeCyan
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contact.phoneNumber.ifBlank { "-" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            if (contact.phoneNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { dialPhoneNumber(context, contact.phoneNumber) }
                            .testTag("btn_call_contact_${contact.id}"),
                        shape = RoundedCornerShape(10.dp),
                        color = KubeCyan.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, KubeCyan.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Telepon ${contact.contactName}",
                                tint = KubeCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Telepon",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { openWhatsApp(context, waDigits, greetingMsg) }
                            .testTag("btn_wa_contact_${contact.id}"),
                        shape = RoundedCornerShape(10.dp),
                        color = KubeGreen.copy(alpha = 0.22f),
                        border = BorderStroke(0.8.dp, KubeGreen.copy(alpha = 0.50f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Message,
                                contentDescription = "WhatsApp ${contact.contactName}",
                                tint = KubeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WhatsApp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun normalizeWhatsappDigits(phoneNumber: String): String {
    val digitsOnly = phoneNumber.filter { it.isDigit() }
    return when {
        digitsOnly.startsWith("0") -> "62" + digitsOnly.drop(1)
        digitsOnly.startsWith("62") -> digitsOnly
        else -> digitsOnly
    }
}

private fun dialPhoneNumber(context: Context, phoneNumber: String) {
    val cleanNumber = phoneNumber.trim()
    runCatching {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Membuka telepon: $cleanNumber", Toast.LENGTH_SHORT).show()
    }
}

private fun openWhatsApp(context: Context, phoneNumberDigits: String, message: String) {
    runCatching {
        val url = "https://wa.me/$phoneNumberDigits?text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Membuka WhatsApp: +$phoneNumberDigits", Toast.LENGTH_SHORT).show()
    }
}
