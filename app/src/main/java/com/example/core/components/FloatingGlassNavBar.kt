package com.example.core.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KubePillNavItem(
    val index: Int,
    val label: String,
    val filledIcon: ImageVector,
    val outlineIcon: ImageVector
)

@Composable
fun FloatingGlassNavBar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = remember {
        listOf(
            KubePillNavItem(0, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
            KubePillNavItem(1, "Delivery", Icons.Rounded.LocalShipping, Icons.Outlined.LocalShipping),
            KubePillNavItem(2, "Map", Icons.Rounded.Map, Icons.Outlined.Map),
            KubePillNavItem(3, "Wallet", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
            KubePillNavItem(4, "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
        )
    }

    val activeAccentColor = Color(0xFF0284A8)
    val activeCircleBg = Color(0xFFE0F4F8)

    val rimBorder = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.65f),
            Color.White.copy(alpha = 0.22f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF0A2029).copy(alpha = 0.30f),
                spotColor = Color(0xFF0A2029).copy(alpha = 0.38f)
            )
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3E606C).copy(alpha = 0.66f),
                        Color(0xFF28434D).copy(alpha = 0.78f)
                    )
                )
            )
            .border(width = 1.2.dp, brush = rimBorder, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag("floating_glass_nav_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = selectedIndex == item.index
                val tabTag = when (item.index) {
                    0 -> "home"
                    1 -> "delivery"
                    2 -> "map"
                    3 -> "wallet"
                    else -> "profile"
                }

                if (isSelected) {
                    // Pil putih mengembang dengan ikon di dalam lingkaran cyan lembut + label tebal
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectTab(item.index) }
                            .animateContentSize(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                            .padding(start = 6.dp, end = 16.dp, top = 5.dp, bottom = 5.dp)
                            .testTag("nav_tab_$tabTag"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(activeCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.filledIcon,
                                contentDescription = item.label,
                                tint = activeAccentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            color = activeAccentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else {
                    // Tombol lingkaran kaca transparan untuk tab tidak aktif
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectTab(item.index) }
                            .testTag("nav_tab_$tabTag"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.outlineIcon,
                            contentDescription = item.label,
                            tint = Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }
    }
}
