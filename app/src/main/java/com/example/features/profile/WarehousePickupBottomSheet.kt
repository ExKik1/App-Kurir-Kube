package com.example.features.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Store
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.components.KubeGlassCard
import com.example.core.components.KubeGlassIconBox
import com.example.core.network.CourierSupabaseService
import com.example.core.network.WarehouseInfo
import com.example.ui.theme.KubeBlueBright
import com.example.ui.theme.KubeTextDark
import com.example.ui.theme.KubeTextMuted

/**
 * ModalBottomSheet bergaya Liquid Glass untuk Informasi Gudang / Pick Up.
 * Menampilkan Nama Gudang, Alamat Lengkap, Jam Operasional, dan tombol Maps
 * yang tersinkronisasi real-time dari tabel `warehouses` Supabase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehousePickupBottomSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val warehouse by CourierSupabaseService.instance
        .streamWarehouseInfo()
        .collectAsState(initial = WarehouseInfo())
    val warehouseName = warehouse.name
    val warehouseAddress = warehouse.address
    val operationalHours = warehouse.operationalHours

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("sheet_informasi_gudang")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Store Icon and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KubeGlassIconBox(
                        icon = Icons.Filled.Store,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        isHighlighted = true,
                        boxSize = 46.dp,
                        iconSize = 24.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Informasi Gudang Es",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                        Text(
                            text = "Titik muat balok & tube es Kube",
                            fontSize = 12.sp,
                            color = KubeTextMuted
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = KubeTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 1. Nama Gudang Card
            KubeGlassCard(
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KubeGlassIconBox(
                        icon = Icons.Filled.Store,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        boxSize = 38.dp,
                        iconSize = 20.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text("Nama Gudang / Depot", fontSize = 11.sp, color = KubeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = warehouseName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KubeTextDark
                        )
                    }
                }
            }

            // 2. Alamat Lengkap Gudang Card
            KubeGlassCard(
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    KubeGlassIconBox(
                        icon = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        boxSize = 38.dp,
                        iconSize = 20.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text("Alamat Lengkap", fontSize = 11.sp, color = KubeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = warehouseAddress,
                            fontSize = 13.sp,
                            color = KubeTextDark,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 3. Jam Operasional Card
            KubeGlassCard(
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KubeGlassIconBox(
                        icon = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        boxSize = 38.dp,
                        iconSize = 20.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text("Jam Operasional Loading Dock", fontSize = 11.sp, color = KubeTextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = operationalHours,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Facilities Info Pill
            KubeGlassCard(
                shape = RoundedCornerShape(14.dp),
                isHighlighted = true,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = KubeBlueBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fasilitas: ${warehouse.facilities}",
                        fontSize = 11.sp,
                        color = Color(0xFF0369A1),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action: Tombol Ikon Maps untuk membuka navigasi rute ke gudang
            Button(
                onClick = {
                    openGoogleMapsRoute(context, warehouse.latitude, warehouse.longitude, warehouseName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = KubeBlueBright),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_open_warehouse_maps")
            ) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka Navigasi Rute ke Gudang (Maps)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_close_warehouse_sheet")
            ) {
                Text("Tutup", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun openGoogleMapsRoute(context: Context, lat: Double, lng: Double, label: String) {
    runCatching {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }.onFailure {
        Toast.makeText(context, "Membuka peta gudang Kube Banyumas", Toast.LENGTH_SHORT).show()
    }
}
