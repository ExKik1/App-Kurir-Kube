package com.example.core.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object KubeImageUtils {
    private val memoryBitmapCache = ConcurrentHashMap<String, ImageBitmap>()

    /**
     * Membuat URI aman via FileProvider untuk menangkap foto langsung dari kamera perangkat.
     */
    fun createTempCameraImageUri(context: Context, prefix: String = "kube_cam"): Uri {
        val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        val tempFile = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    /**
     * Mengonversi gambar dari format apa pun (JPG, PNG, WEBP, HEIC) menjadi file vektor SVG (.svg)
     * siap unggah ke Supabase Storage.
     */
    fun convertAnyImageToSvgBytes(
        context: Context?,
        photoUriString: String,
        maxDimensionPx: Int = 1024
    ): ByteArray? {
        return runCatching {
            val rawBytes: ByteArray? = when {
                photoUriString.startsWith("http://") || photoUriString.startsWith("https://") -> {
                    URL(photoUriString).openStream().use { it.readBytes() }
                }
                (photoUriString.startsWith("content://") || photoUriString.startsWith("android.resource://")) && context != null -> {
                    context.contentResolver.openInputStream(Uri.parse(photoUriString))?.use { it.readBytes() }
                }
                photoUriString.startsWith("file://") -> {
                    File(Uri.parse(photoUriString).path.orEmpty()).takeIf { it.exists() }?.readBytes()
                }
                else -> File(photoUriString).takeIf { it.exists() }?.readBytes()
            }

            if (rawBytes == null || rawBytes.isEmpty()) return null

            // Jika input sudah berupa SVG, kembalikan langsung
            val headerSnippet = rawBytes.take(120).toByteArray().toString(Charsets.UTF_8)
            if (headerSnippet.contains("<svg", ignoreCase = true)) {
                return rawBytes
            }

            val originalBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: return null

            // Skala proporsional agar ukuran SVG ringan & tajam saat dimuat
            val origW = originalBitmap.width.coerceAtLeast(1)
            val origH = originalBitmap.height.coerceAtLeast(1)
            val scale = minOf(
                1.0f,
                maxDimensionPx.toFloat() / maxOf(origW, origH).toFloat()
            )
            val targetW = (origW * scale).toInt().coerceAtLeast(1)
            val targetH = (origH * scale).toInt().coerceAtLeast(1)

            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
            } else {
                originalBitmap
            }

            val jpegStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, jpegStream)
            val base64Data = Base64.encodeToString(jpegStream.toByteArray(), Base64.NO_WRAP)

            val svgContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$targetW" height="$targetH" viewBox="0 0 $targetW $targetH">
                  <image width="$targetW" height="$targetH" preserveAspectRatio="xMidYMid meet" href="data:image/jpeg;base64,$base64Data" xlink:href="data:image/jpeg;base64,$base64Data"/>
                </svg>
            """.trimIndent()

            svgContent.toByteArray(Charsets.UTF_8)
        }.getOrNull()
    }

    /**
     * Memuat gambar dari URL Supabase (mendukung .svg maupun .jpg/.png/.webp) atau URI lokal.
     */
    suspend fun loadImageBitmap(context: Context, source: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            if (source.isBlank()) return@withContext null
            memoryBitmapCache[source]?.let { return@withContext it }

            runCatching {
                val bytes: ByteArray? = when {
                    source.startsWith("http://") || source.startsWith("https://") -> {
                        val conn = (URL(source).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 10_000
                            readTimeout = 15_000
                            instanceFollowRedirects = true
                        }
                        conn.inputStream.use { it.readBytes() }
                    }
                    source.startsWith("content://") || source.startsWith("android.resource://") -> {
                        context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
                    }
                    source.startsWith("file://") -> {
                        File(Uri.parse(source).path.orEmpty()).takeIf { it.exists() }?.readBytes()
                    }
                    else -> File(source).takeIf { it.exists() }?.readBytes()
                }

                if (bytes == null || bytes.isEmpty()) return@withContext null

                val textCheck = bytes.take(300).toByteArray().toString(Charsets.UTF_8)
                val rasterBytes = if (textCheck.contains("<svg", ignoreCase = true)) {
                    val fullSvg = bytes.toString(Charsets.UTF_8)
                    val regex = Regex("""data:image/[a-zA-Z0-9+.-]+;base64,([A-Za-z0-9+/=\s]+)""")
                    val base64Part = regex.find(fullSvg)?.groupValues?.getOrNull(1)
                        ?.replace("\\s".toRegex(), "")
                    if (!base64Part.isNullOrBlank()) {
                        Base64.decode(base64Part, Base64.DEFAULT)
                    } else null
                } else {
                    bytes
                } ?: return@withContext null

                BitmapFactory.decodeByteArray(rasterBytes, 0, rasterBytes.size)
                    ?.asImageBitmap()
                    ?.also { memoryBitmapCache[source] = it }
            }.getOrNull()
        }
}

/**
 * Komponen gambar universal yang merender URL .svg maupun .jpg/.png dari Supabase Storage.
 */
@Composable
fun KubeSupabaseImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    backgroundColor: Color = Color(0xFFF1F5F9)
) {
    val context = LocalContext.current
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(imageUrl) { mutableStateOf(true) }

    LaunchedEffect(imageUrl) {
        isLoading = true
        bitmap = if (!imageUrl.isNullOrBlank()) {
            KubeImageUtils.loadImageBitmap(context, imageUrl)
        } else {
            null
        }
        isLoading = false
    }

    Box(
        modifier = modifier.background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier.matchParentSize()
                )
            }
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    color = Color(0xFF0284A8),
                    strokeWidth = 2.5.dp
                )
            }
            else -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Foto tidak tersedia",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
