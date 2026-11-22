package com.example.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.network.ChatMessageRecord
import com.example.core.network.CourierSupabaseService
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Komponen ruang obrolan In-App Chat real-time bergaya Liquid Glass.
 * Mendukung 2 roomType:
 * - "courier_admin": Live Chat Kurir <-> Admin Operasional Kube
 * - "order_chat": Live Chat Kurir <-> Pelanggan pada pesanan aktif
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppChatBottomSheet(
    title: String,
    subtitle: String,
    roomType: String,
    orderCode: String? = null,
    onDismiss: () -> Unit
) {
    val supabaseService = remember { CourierSupabaseService.instance }
    val adminMessages by supabaseService.adminChatMessages.collectAsState()
    val orderMessagesMap by supabaseService.orderChatMessages.collectAsState()

    val orderDbId = remember(orderCode, orderMessagesMap) {
        orderCode?.let { supabaseService.getOrderDbId(it) }
    }

    val messages = remember(roomType, adminMessages, orderMessagesMap, orderDbId) {
        if (roomType == "order_chat") {
            orderDbId?.let { orderMessagesMap[it] }.orEmpty()
        } else {
            adminMessages
        }
    }

    var messageInput by remember { mutableStateOf("") }
    var isInitialLoading by remember { mutableStateOf(messages.isEmpty()) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val quickReplies = remember(roomType) {
        if (roomType == "order_chat") {
            listOf(
                "Saya sedang menuju lokasi Anda",
                "Saya sudah tiba di depan lokasi",
                "Mohon konfirmasi titik pengantaran"
            )
        } else {
            listOf(
                "Mohon bantuan kendala pengiriman",
                "Konfirmasi pesanan pelanggan"
            )
        }
    }

    // Sinkronisasi cepat setiap 2 detik selama sheet terbuka & tandai terbaca
    LaunchedEffect(roomType, orderCode) {
        while (isActive) {
            runCatching {
                if (roomType == "order_chat" && !orderCode.isNullOrBlank()) {
                    supabaseService.fetchOrderChatMessages(orderCode)
                    supabaseService.markChatMessagesAsRead("order_chat", orderCode)
                } else {
                    supabaseService.fetchAdminChatMessages()
                    supabaseService.markChatMessagesAsRead("courier_admin")
                }
            }
            isInitialLoading = false
            delay(2000L)
        }
    }

    // Auto-scroll ke pesan terbaru
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendChatMessage(rawText: String, clearInputOnSuccess: Boolean) {
        val clean = rawText.trim()
        if (clean.isEmpty() || isSending) return
        isSending = true
        errorMessage = null
        coroutineScope.launch {
            val result = if (roomType == "order_chat" && !orderCode.isNullOrBlank()) {
                supabaseService.sendOrderChatMessage(orderCode, clean, context = context)
            } else {
                supabaseService.sendAdminChatMessage(clean, context = context)
            }
            isSending = false
            result.fold(
                onSuccess = {
                    if (clearInputOnSuccess) {
                        messageInput = ""
                    }
                },
                onFailure = { err ->
                    errorMessage = err.message ?: "Gagal mengirim pesan. Coba lagi."
                }
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF0F9FF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        modifier = Modifier
            .imePadding()
            .testTag("in_app_chat_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE0F4FF),
                            Color(0xFFF4FBFF),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
                .navigationBarsPadding()
        ) {
            // Drag Handle + Liquid Glass Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF022B49),
                                Color(0xFF005C8A),
                                Color(0xFF0083B0)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, KubeCyan.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (roomType == "order_chat") Icons.Filled.Person else Icons.Filled.SupportAgent,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF00E676).copy(alpha = 0.22f),
                                border = BorderStroke(0.8.dp, Color(0xFF00E676).copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "Live",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB9F6CA),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("btn_close_chat_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup Obrolan",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Messages Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 380.dp)
            ) {
                when {
                    isInitialLoading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = KubeBlueBright,
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            DbBodyText(
                                text = "Memuat riwayat percakapan...",
                                color = KubeMutedText
                            )
                        }
                    }

                    messages.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0083B0).copy(alpha = 0.12f))
                                    .border(1.dp, Color(0xFF00B4DB).copy(alpha = 0.30f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF0083B0),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            DbTitleText(
                                text = "Belum ada pesan",
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DbBodyText(
                                text = "Pilih pesan cepat di bawah atau ketik pesan untuk memulai percakapan secara real-time.",
                                maxLines = 2,
                                color = KubeMutedText
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_message_list"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = messages,
                                key = { "${it.id}_${it.timeLabel}_${it.message.hashCode()}" }
                            ) { msg ->
                                ChatMessageBubble(message = msg)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFBAE6FD).copy(alpha = 0.65f))

            // Quick Reply Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickReplies.forEachIndexed { index, replyText ->
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !isSending) {
                                sendChatMessage(replyText, clearInputOnSuccess = false)
                            }
                            .testTag("quick_reply_chip_$index"),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFF0083B0).copy(alpha = 0.35f)),
                        shadowElevation = 1.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = replyText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF006D96),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                )
            }

            // Message Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = {
                        messageInput = it
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = if (roomType == "order_chat") {
                                "Tulis pesan ke pelanggan..."
                            } else {
                                "Tulis pesan ke Admin Kube..."
                            },
                            fontSize = 13.sp,
                            color = KubeMutedText
                        )
                    },
                    maxLines = 3,
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = { sendChatMessage(messageInput, clearInputOnSuccess = true) }
                    ),
                    shape = RoundedCornerShape(22.dp),
                    colors = kubeLightTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                Spacer(modifier = Modifier.width(10.dp))

                val canSend = messageInput.isNotBlank() && !isSending
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(if (canSend) 4.dp else 0.dp, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = canSend) {
                            sendChatMessage(messageInput, clearInputOnSuccess = true)
                        }
                        .testTag("btn_send_chat"),
                    shape = CircleShape,
                    color = if (canSend) Color(0xFF0083B0) else Color(0xFFCBD5E1)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSending) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim Pesan",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessageRecord) {
    val isFromMe = message.isFromMe
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        if (isFromMe) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(3.dp, RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                    .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0083B0),
                                Color(0xFF00A8D6)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = message.message,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = message.timeLabel,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.80f),
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = if (message.isRead) Color(0xFFB9F6CA) else Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
                color = Color.White.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD).copy(alpha = 0.75f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = message.senderName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0083B0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val roleTag = when (message.senderRole.lowercase()) {
                            "admin" -> "Admin"
                            "consumer" -> "Pelanggan"
                            else -> ""
                        }
                        if (roleTag.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFE0F2FE)
                            ) {
                                Text(
                                    text = roleTag,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.message,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = KubePrimaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timeLabel,
                        fontSize = 10.sp,
                        color = KubeMutedText,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}
