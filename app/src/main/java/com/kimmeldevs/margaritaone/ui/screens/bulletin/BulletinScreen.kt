package com.kimmeldevs.margaritaone.ui.screens.bulletin

import androidx.compose.material.icons.filled.Edit
import com.kimmeldevs.margaritaone.ui.components.FilterPill
import com.kimmeldevs.margaritaone.ui.components.GradientFab
import com.kimmeldevs.margaritaone.ui.components.ScreenHeader
import com.kimmeldevs.margaritaone.i18n.AppLanguage
import com.kimmeldevs.margaritaone.i18n.I18n
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.t
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.data.model.PostDto
import com.kimmeldevs.margaritaone.data.model.PostType
import com.kimmeldevs.margaritaone.ui.components.CommentsSheet
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog
import com.kimmeldevs.margaritaone.ui.components.EmptyListText
import com.kimmeldevs.margaritaone.ui.components.SearchTopBar
import com.kimmeldevs.margaritaone.ui.components.SkeletonList
import com.kimmeldevs.margaritaone.ui.components.ImageGalleryDialog
import com.kimmeldevs.margaritaone.ui.components.PostCard
import com.kimmeldevs.margaritaone.ui.components.RefreshOnResume
import com.kimmeldevs.margaritaone.ui.components.UiStateContent
import com.kimmeldevs.margaritaone.ui.viewmodel.BulletinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulletinScreen(
    canPost: Boolean,
    currentUserId: String,
    onNewPostClick: () -> Unit,
    onEditPost: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Profile -> "Posts I reacted to": same cards, only posts this user reacted to. */
    reactedOnly: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    viewModel: BulletinViewModel = viewModel(factory = BulletinViewModel.factory(currentUserId, reactedOnly))
) {
    val state by viewModel.posts.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.posts.isRefreshing.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val language = LocalLanguage.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<PostDto?>(null) }
    val reactions by viewModel.reactions.collectAsStateWithLifecycle()
    val commentCounts by viewModel.commentCounts.collectAsStateWithLifecycle()
    var commentsFor by remember { mutableStateOf<PostDto?>(null) }
    // (photo urls, index tapped) for the full-screen gallery
    var gallery by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(I18n.tr(language, it))
            viewModel.clearMessage()
        }
    }

    RefreshOnResume { viewModel.posts.refreshIfLoaded() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                eyebrow = "Santa Margarita",
                title = if (reactedOnly) "Posts I reacted to" else "Bulletin",
                query = query,
                onQueryChange = viewModel::setQuery,
                searchPlaceholder = "Search announcements",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canPost && !reactedOnly) {
                GradientFab(text = t("New post"), icon = Icons.Filled.Edit, onClick = onNewPostClick)
            }
        }
    ) { innerPadding ->
        UiStateContent(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.posts.refresh() },
            modifier = Modifier.padding(innerPadding),
            loading = { SkeletonList() }
        ) { posts ->
            val visible = viewModel.visiblePosts(posts, filter, query)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterPill(label = t("All"), selected = filter == null, onClick = { viewModel.setFilter(null) })
                        }
                        items(PostType.entries) { type ->
                            FilterPill(label = t(type.label), selected = filter == type, onClick = { viewModel.setFilter(type) })
                        }
                    }
                }
                if (visible.isEmpty()) {
                    item {
                        EmptyListText(
                            when {
                                posts.isNotEmpty() -> "No posts match your search."
                                reactedOnly -> "You haven't reacted to any posts yet. Tap Like on a post, or long-press it for more reactions."
                                else -> "No posts yet."
                            }
                        )
                    }
                }
                items(visible, key = { it.id }) { post ->
                    val imageUrls = viewModel.imageUrls(post)
                    PostCard(
                        post = post,
                        imageUrls = imageUrls,
                        documentUrl = viewModel.documentUrl(post),
                        reactions = viewModel.summary(post.id, reactions),
                        commentCount = commentCounts[post.id] ?: 0,
                        onComment = { commentsFor = post },
                        onOpenDocument = { url ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        },
                        onViewImage = { index -> gallery = imageUrls to index },
                        onReact = { type -> viewModel.react(post.id, type) },
                        onShare = { sharePost(context, post, language) },
                        onEdit = if (canPost) ({ onEditPost(post.id) }) else null,
                        onDelete = if (canPost) ({ pendingDelete = post }) else null
                    )
                }
            }
        }
    }

    pendingDelete?.let { post ->
        ConfirmDialog(
            title = t("Delete post?"),
            message = t("\"%s\" will be removed from the bulletin for everyone.", post.title),
            confirmLabel = t("Delete"),
            destructive = true,
            onConfirm = {
                viewModel.deletePost(post)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
    commentsFor?.let { post ->
        CommentsSheet(
            postId = post.id,
            postTitle = post.title,
            currentUserId = currentUserId,
            isAdmin = canPost,
            onDismiss = { commentsFor = null }
        )
    }
    gallery?.let { (urls, index) ->
        ImageGalleryDialog(urls = urls, startIndex = index, onDismiss = { gallery = null })
    }
}

/** Share to Messenger, SMS, etc. (#15) */
private fun sharePost(context: Context, post: PostDto, language: AppLanguage) {
    val chooserTitle = I18n.tr(language, "Share announcement")
    val text = buildString {
        append("[${I18n.tr(language, post.type.label)}] ${post.title}")
        if (post.body.isNotBlank()) append("\n\n${post.body}")
        append("\n\n- LGU Santa Margarita, via MargaritaOne")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
