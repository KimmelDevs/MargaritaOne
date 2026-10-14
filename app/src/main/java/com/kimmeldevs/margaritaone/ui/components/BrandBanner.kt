package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.R
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.theme.BrandBlue
import com.kimmeldevs.margaritaone.ui.theme.BrandRed

/** Header in the style of the municipal banner: a red diagonal stripe and blue triangle on light grey, with the mascot. */
@Composable
fun BrandBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Color(0xFFE9EAEE))
            // Soft lighter band across the middle, like the banner's centre strip.
            drawRect(Color(0xFFF5F5F7), topLeft = Offset(0f, h * 0.18f), size = size.copy(height = h * 0.64f))
            drawPath(
                Path().apply {
                    moveTo(w * 0.15f, h); lineTo(w * 0.29f, h * 0.30f); lineTo(w * 0.43f, h); close()
                },
                color = BrandBlue
            )
            drawPath(
                Path().apply {
                    moveTo(w * 0.04f, h); lineTo(w * 0.24f, 0f); lineTo(w * 0.34f, 0f); lineTo(w * 0.14f, h); close()
                },
                color = BrandRed
            )
        }
        Image(
            painter = painterResource(R.drawable.mascot),
            contentDescription = t("MargaritaOne mascot"),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(136.dp)
        )
    }
}
