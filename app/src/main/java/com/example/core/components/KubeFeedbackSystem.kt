package com.example.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class KubeFeedbackType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class KubeFeedbackEvent(
    val id: Long = System.nanoTime(),
    val type: KubeFeedbackType,
    val title: String,
    val message: String,
    val durationMillis: Long = 3500L
)

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Global Liquid Glass in-app feedback manager.
 * Dapat dipanggil dari ViewModel, Service, maupun Composable manapun di aplikasi Kurir Kube.
 */
object KubeFeedbackManager {
    private val _currentEvent = MutableStateFlow<KubeFeedbackEvent?>(null)
    val currentEvent: StateFlow<KubeFeedbackEvent?> = _currentEvent.asStateFlow()

    fun showSuccess(title: String, message: String, durationMillis: Long = 3400L) {
        _currentEvent.value = KubeFeedbackEvent(
            type = KubeFeedbackType.SUCCESS,
            title = title,
            message = message,
            durationMillis = durationMillis
        )
    }

    fun showError(title: String, message: String, durationMillis: Long = 4200L) {
        _currentEvent.value = KubeFeedbackEvent(
            type = KubeFeedbackType.ERROR,
            title = title,
            message = message,
            durationMillis = durationMillis
        )
    }

    fun showWarning(title: String, message: String, durationMillis: Long = 3800L) {
        _currentEvent.value = KubeFeedbackEvent(
            type = KubeFeedbackType.WARNING,
            title = title,
            message = message,
            durationMillis = durationMillis
        )
    }

    fun showInfo(title: String, message: String, durationMillis: Long = 3400L) {
        _currentEvent.value = KubeFeedbackEvent(
            type = KubeFeedbackType.INFO,
            title = title,
            message = message,
            durationMillis = durationMillis
        )
    }

    fun dismiss() {
        _currentEvent.value = null
    }
}

/**
 * Root wrapper untuk menampilkan notifikasi mengambang Liquid Glass di atas seluruh layar.
 */
@Composable
fun KubeGlassFeedbackHost(
    content: @Composable () -> Unit
) {
    val currentEvent by KubeFeedbackManager.currentEvent.collectAsState()

    LaunchedEffect(currentEvent?.id) {
        val active = currentEvent ?: return@LaunchedEffect
        delay(active.durationMillis)
        if (KubeFeedbackManager.currentEvent.value?.id == active.id) {
            KubeFeedbackManager.dismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = currentEvent != null,
            enter = slideInVertically(initialOffsetY = { -it - 40 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it - 40 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .zIndex(999f)
        ) {
            currentEvent?.let { event ->
                KubeGlassFeedbackBanner(
                    event = event,
                    onDismiss = { KubeFeedbackManager.dismiss() }
                )
            }
        }
    }
}

@Composable
fun KubeGlassFeedbackBanner(
    event: KubeFeedbackEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = when (event.type) {
        KubeFeedbackType.SUCCESS -> Quadruple(
            Icons.Rounded.CheckCircle,
            Color(0xFF059669),
            Color(0xFFECFDF5).copy(alpha = 0.94f),
            Color(0xFF10B981).copy(alpha = 0.45f)
        )
        KubeFeedbackType.ERROR -> Quadruple(
            Icons.Rounded.Error,
            Color(0xFFDC2626),
            Color(0xFFFEF2F2).copy(alpha = 0.95f),
            Color(0xFFEF4444).copy(alpha = 0.45f)
        )
        KubeFeedbackType.WARNING -> Quadruple(
            Icons.Rounded.Warning,
            Color(0xFFD97706),
            Color(0xFFFFFBEB).copy(alpha = 0.95f),
            Color(0xFFF59E0B).copy(alpha = 0.48f)
        )
        KubeFeedbackType.INFO -> Quadruple(
            Icons.Rounded.Info,
            Color(0xFF0284C7),
            Color(0xFFF0F9FF).copy(alpha = 0.94f),
            Color(0xFF38BDF8).copy(alpha = 0.48f)
        )
    }

    val icon = palette.first
    val accentColor = palette.second
    val glassTint = palette.third
    val rimColor = palette.fourth
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.25f),
                spotColor = Color(0xFF0F172A).copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.96f),
                        glassTint
                    )
                )
            )
            .border(
                width = 1.25.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        rimColor
                    )
                ),
                shape = shape
            )
            .clickable { onDismiss() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("kube_feedback_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.14f))
                    .border(1.dp, accentColor.copy(alpha = 0.28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = event.title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E3A4C),
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("kube_feedback_dismiss")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Tutup notifikasi",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Dialog Pop-up Liquid Glass terstandar pengganti AlertDialog polos/gelap.
 * Memberikan kontras teks gelap tajam di atas panel kaca es terang dengan specular rim.
 */
@Composable
fun KubeGlassDialog(
    onDismiss: () -> Unit,
    icon: ImageVector,
    iconTint: Color = Color(0xFF0284C7),
    title: String,
    message: String,
    confirmText: String = "OK",
    confirmButtonColor: Color = Color(0xFF0284C7),
    onConfirm: () -> Unit,
    dismissText: String? = "Batal"
) {
    val dialogShape = RoundedCornerShape(28.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = dialogShape,
                    ambientColor = Color(0xFF0F3657).copy(alpha = 0.22f),
                    spotColor = Color(0xFF0F172A).copy(alpha = 0.25f)
                )
                .clip(dialogShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.97f),
                            Color(0xFFF0F9FF).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFBAE6FD).copy(alpha = 0.85f)
                        )
                    ),
                    shape = dialogShape
                )
                .padding(24.dp)
                .testTag("kube_glass_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.13f))
                        .border(1.2.dp, iconTint.copy(alpha = 0.30f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color(0xFF1E3A4C),
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!dismissText.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.85f),
                                contentColor = Color(0xFF334155)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("kube_dialog_dismiss")
                        ) {
                            Text(
                                text = dismissText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmButtonColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("kube_dialog_confirm")
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
