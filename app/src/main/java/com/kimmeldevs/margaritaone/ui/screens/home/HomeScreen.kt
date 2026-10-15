package com.kimmeldevs.margaritaone.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.R
import com.kimmeldevs.margaritaone.data.model.PostDto
import com.kimmeldevs.margaritaone.data.model.PostType
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.EmptyListText
import com.kimmeldevs.margaritaone.ui.components.IconBadge
import com.kimmeldevs.margaritaone.ui.components.MargaritaOneLogo
import com.kimmeldevs.margaritaone.ui.components.RefreshOnResume
import com.kimmeldevs.margaritaone.ui.components.SectionHeader
import com.kimmeldevs.margaritaone.ui.theme.DeepNavy
import com.kimmeldevs.margaritaone.ui.theme.PrimaryBlue
import com.kimmeldevs.margaritaone.ui.theme.PrimaryRed
import com.kimmeldevs.margaritaone.ui.viewmodel.HomeViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.UiState
import com.kimmeldevs.margaritaone.util.Dates
import kotlinx.coroutines.delay
import java.util.Calendar

/** A shortcut on the home grid. */
data class HomeTile(val label: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit)

private val HERO_PHOTOS = listOf(R.drawable.hero_town, R.drawable.hero_welcome)

/** Home tab: greeting banner, shortcuts to the app's features, and the latest bulletin posts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tiles: List<HomeTile>,
    onOpenBulletin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val state by viewModel.latest.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.latest.isRefreshing.collectAsStateWithLifecycle()
    RefreshOnResume { viewModel.latest.refreshIfLoaded() }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.latest.refresh() },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        MargaritaOneLogo(logoHeight = 26.dp)
                        Text(
                            t("STA. MARGARITA, SAMAR"),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenBulletin) {
                        Icon(Icons.Outlined.Notifications, contentDescription = t("Announcements"), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            item { HeroCarousel(modifier = Modifier.padding(16.dp)) }
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    tiles.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { tile -> TileCard(tile, Modifier.weight(1f)) }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item {
                SectionHeader(
                    title = t("Latest Updates"),
                    action = t("View all"),
                    onAction = onOpenBulletin,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp)
                )
            }
            when (val s = state) {
                UiState.Loading -> item { EmptyListText("Loading…") }
                is UiState.Error -> item { EmptyListText(s.message) }
                is UiState.Success -> if (s.data.isEmpty()) {
                    item { EmptyListText("No announcements yet.") }
                } else {
                    s.data.forEach { post ->
                        item(key = post.id) {
                            UpdateCard(post, onClick = onOpenBulletin, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** Photo banner with a time-of-day greeting; swipes between town photos and advances by itself. */
@Composable
private fun HeroCarousel(modifier: Modifier = Modifier) {
    val pager = rememberPagerState { HERO_PHOTOS.size }
    LaunchedEffect(pager) {
        while (true) {
            delay(6_000)
            pager.animateScrollToPage((pager.currentPage + 1) % HERO_PHOTOS.size)
        }
    }
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        else -> "Good evening,"
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 8.5f)
            .clip(MaterialTheme.shapes.large)
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Image(
                painter = painterResource(HERO_PHOTOS[page]),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Navy wash on the left so the white text stays readable over any photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(DeepNavy.copy(alpha = 0.92f), PrimaryBlue.copy(alpha = 0.35f), Color.Transparent)))
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(20.dp)
                .fillMaxWidth(0.62f)
        ) {
            Text(t(greeting), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
            Text(t("Sta. Margarita!"), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text(
                t("Your municipality, closer than ever."),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        // Red/blue ribbon along the bottom edge, as in the banner.
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(26.dp)
        ) {
            val w = size.width
            val h = size.height
            drawPath(Path().apply {
                moveTo(0f, h * 0.2f); cubicTo(w * 0.3f, h * 1.1f, w * 0.7f, h * 1.0f, w, h * 0.1f)
                lineTo(w, h); lineTo(0f, h); close()
            }, PrimaryRed)
            drawPath(Path().apply {
                moveTo(w * 0.45f, h); cubicTo(w * 0.7f, h * 0.9f, w * 0.85f, h * 0.6f, w, h * 0.2f)
                lineTo(w, h); close()
            }, PrimaryBlue)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 30.dp)
        ) {
            repeat(HERO_PHOTOS.size) { index ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == pager.currentPage) 18.dp else 7.dp, height = 7.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (index == pager.currentPage) 1f else 0.5f))
                )
            }
        }
    }
}

@Composable
private fun TileCard(tile: HomeTile, modifier: Modifier = Modifier) {
    Card(
        onClick = tile.onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp)
        ) {
            IconBadge(tile.icon, tile.tint, size = 52.dp)
            Text(
                t(tile.label),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun PostType.icon(): ImageVector = when (this) {
    PostType.ANNOUNCEMENT -> Icons.Filled.Campaign
    PostType.ORDINANCE -> Icons.Filled.Gavel
    PostType.EVENT -> Icons.Filled.Event
    PostType.EMERGENCY -> Icons.Filled.Warning
}

private fun PostType.tint(): Color = when (this) {
    PostType.ANNOUNCEMENT, PostType.EMERGENCY -> PrimaryRed
    PostType.ORDINANCE -> DeepNavy
    PostType.EVENT -> PrimaryBlue
}

@Composable
private fun UpdateCard(post: PostDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(post.type.tint())
            ) {
                Icon(post.type.icon(), contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    t(post.type.label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(post.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (post.body.isNotBlank()) {
                    Text(
                        post.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Dates.date(post.publishedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
