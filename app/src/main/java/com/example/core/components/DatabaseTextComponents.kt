package com.example.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val KubePrimaryText = Color(0xFF0F172A)   // Slate 900 - Tajam & Kontras Tinggi
val KubeSecondaryText = Color(0xFF334155) // Slate 700 - Alamat & Deskripsi Item
val KubeMutedText = Color(0xFF64748B)     // Slate 500 - Label & Timestamp

@Composable
fun DbTitleText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    color: Color = KubePrimaryText
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun DbBodyText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    color: Color = KubeSecondaryText
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun DbCurrencyOrMetricText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF0284C7),
    fontSize: TextUnit = 16.sp
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        style = TextStyle(fontFeatureSettings = "tnum"), // Tabular figures agar angka sejajar
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun DbStatusPill(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
