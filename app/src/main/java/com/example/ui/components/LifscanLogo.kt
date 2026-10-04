package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

val LifscanNavyDark = Color(0xFF0D1129)
val LifscanGold = Color(0xFFFBBF24)
val LifscanGreen = Color(0xFF22C55E)
val LifscanTeal = Color(0xFF06B6D4)

@Composable
fun LifscanHeroLogo(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    showText: Boolean = true
) {
    Column(
        modifier = modifier.testTag("lifscan_hero_logo"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .size(size)
                .shadow(12.dp, RoundedCornerShape(26.dp))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(LifscanGold.copy(alpha = 0.8f), LifscanGreen.copy(alpha = 0.8f), LifscanTeal.copy(alpha = 0.8f))
                    ),
                    shape = RoundedCornerShape(26.dp)
                ),
            shape = RoundedCornerShape(26.dp),
            color = LifscanNavyDark
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lifscan_logo),
                    contentDescription = "Lifscan Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (showText) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "L I F S C A N",
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 5.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "HEALTH & EMERGENCY INTELLIGENCE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TealPrimary
            )
        }
    }
}

@Composable
fun LifscanMiniBadge(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    Surface(
        modifier = modifier
            .size(size)
            .shadow(4.dp, CircleShape)
            .border(1.5.dp, LifscanTeal.copy(alpha = 0.6f), CircleShape),
        shape = CircleShape,
        color = LifscanNavyDark
    ) {
        Image(
            painter = painterResource(id = R.drawable.lifscan_logo),
            contentDescription = "Lifscan Badge",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun LifscanAppBarBrand(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LifscanMiniBadge(size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
