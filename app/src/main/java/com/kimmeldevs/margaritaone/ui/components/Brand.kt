package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kimmeldevs.margaritaone.R
import com.kimmeldevs.margaritaone.ui.theme.BrandGradient
import com.kimmeldevs.margaritaone.ui.theme.PrimaryBlue
import com.kimmeldevs.margaritaone.ui.theme.PrimaryRed

/** "Margarita" in the heading colour and "One" in brand red. */
@Composable
fun Wordmark(fontSize: TextUnit = 22.sp, modifier: Modifier = Modifier) {
    val navy = MaterialTheme.colorScheme.onSurface
    val red = MaterialTheme.colorScheme.primary
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = navy)) { append("Margarita") }
            withStyle(SpanStyle(color = red)) { append("One") }
        },
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/** The red/blue "M" with the wordmark beside it (top bars) or below it ([stacked], login). */
@Composable
fun MargaritaOneLogo(modifier: Modifier = Modifier, stacked: Boolean = false, logoHeight: Dp = 28.dp) {
    if (stacked) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.logo_m), contentDescription = null, modifier = Modifier.height(logoHeight))
            Wordmark(fontSize = 34.sp)
        }
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(painterResource(R.drawable.logo_m), contentDescription = null, modifier = Modifier.height(logoHeight))
            Wordmark(fontSize = 20.sp)
        }
    }
}

/** Primary action: pill with the signature red→blue gradient. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    showArrow: Boolean = false
) {
    val active = enabled && !loading
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(if (active) 6.dp else 0.dp, CircleShape, ambientColor = PrimaryBlue, spotColor = PrimaryRed)
            .clip(CircleShape)
            .background(BrandGradient)
            .alpha(if (active) 1f else 0.6f)
            .clickable(enabled = active, onClick = onClick)
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
                if (showArrow) Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}

/** Rounded square with a tinted background and an icon, as on the home tiles and hotline cards. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(tint.copy(alpha = 0.12f))
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Red and blue ribbons along the bottom of a screen (login footer, bottom bar). */
@Composable
fun BrandWaves(modifier: Modifier = Modifier, height: Dp = 56.dp) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val red = Path().apply {
            moveTo(0f, h * 0.15f)
            cubicTo(w * 0.25f, h * 0.55f, w * 0.55f, h * 1.0f, w, h * 0.55f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(red, PrimaryRed)
        val blue = Path().apply {
            moveTo(0f, h * 0.85f)
            cubicTo(w * 0.35f, h * 1.05f, w * 0.65f, h * 0.55f, w, h * 0.25f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(blue, PrimaryBlue)
    }
}

/** Section heading with an optional "View all" style action on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
