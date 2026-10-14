package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.kimmeldevs.margaritaone.i18n.t

private val GAP = 2.dp

/**
 * Facebook-style photo layout:
 * 1 photo full width · 2 side by side · 3 = one big on top + two below ·
 * 4 = 2×2 grid · 5+ = two on top, three below, with "+N" over the last one.
 */
@Composable
fun PostImageGrid(
    urls: List<String>,
    onImageClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (urls.isEmpty()) return
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        verticalArrangement = Arrangement.spacedBy(GAP)
    ) {
        when (urls.size) {
            1 -> Tile(urls[0], 0, onImageClick, Modifier.fillMaxWidth().aspectRatio(4f / 3f))
            2 -> TileRow(urls, listOf(0, 1), onImageClick, aspect = 2f)
            3 -> {
                Tile(urls[0], 0, onImageClick, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                TileRow(urls, listOf(1, 2), onImageClick, aspect = 2f)
            }
            4 -> {
                TileRow(urls, listOf(0, 1), onImageClick, aspect = 2f)
                TileRow(urls, listOf(2, 3), onImageClick, aspect = 2f)
            }
            else -> {
                TileRow(urls, listOf(0, 1), onImageClick, aspect = 2f)
                TileRow(urls, listOf(2, 3, 4), onImageClick, aspect = 3f, extraCount = urls.size - 5)
            }
        }
    }
}

@Composable
private fun TileRow(
    urls: List<String>,
    indices: List<Int>,
    onImageClick: (Int) -> Unit,
    aspect: Float,
    extraCount: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspect),
        horizontalArrangement = Arrangement.spacedBy(GAP)
    ) {
        indices.forEachIndexed { position, index ->
            val isLast = position == indices.lastIndex
            Tile(
                url = urls[index],
                index = index,
                onImageClick = onImageClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                overlayCount = if (isLast) extraCount else 0
            )
        }
    }
}

@Composable
private fun Tile(
    url: String,
    index: Int,
    onImageClick: (Int) -> Unit,
    modifier: Modifier,
    overlayCount: Int = 0
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onImageClick(index) },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = t("Photo %d", index + 1),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (overlayCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text("+$overlayCount", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Full-screen photo viewer: swipe between photos, pinch or double-tap to zoom. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGalleryDialog(
    urls: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val pagerState = rememberPagerState(initialPage = startIndex) { urls.size }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableImage(url = urls[page])
            }
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = t("Close"))
            }
            if (urls.size > 1) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .safeDrawingPadding()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${urls.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pinch / double-tap zoom that leaves one-finger swipes to the pager while not zoomed in.
 */
@Composable
private fun ZoomableImage(url: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f; offset = Offset.Zero
                    } else {
                        scale = 2.5f
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multiTouch = event.changes.size > 1
                        if (multiTouch || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + event.calculatePan()
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
