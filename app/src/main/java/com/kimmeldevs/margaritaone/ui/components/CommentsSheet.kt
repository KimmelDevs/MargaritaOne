package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.data.model.PostComment
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.viewmodel.CommentsViewModel
import com.kimmeldevs.margaritaone.util.Dates

/** Comments on a post (#7). Everyone can delete their own; admins can remove any (moderation). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    postTitle: String,
    currentUserId: String,
    isAdmin: Boolean,
    onDismiss: () -> Unit
) {
    val viewModel: CommentsViewModel = viewModel(key = "comments_$postId", factory = CommentsViewModel.factory(postId))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by rememberSaveable(postId) { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<PostComment?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.imePadding()) {
            Text(
                postTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                t("Comments (%d)", state.comments.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 8.dp)
            )
            HorizontalDivider()

            Box(modifier = Modifier.heightIn(min = 120.dp, max = 420.dp)) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center).padding(24.dp))
                    state.comments.isEmpty() -> EmptyListText("No comments yet. Be the first to comment.")
                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        items(state.comments, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                isMine = comment.userId == currentUserId,
                                canDelete = comment.userId == currentUserId || isAdmin,
                                onDelete = { pendingDelete = comment }
                            )
                        }
                    }
                }
            }

            state.error?.let {
                Text(t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 20.dp))
            }
            HorizontalDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(1000) },
                    placeholder = { Text(t("Write a comment…")) },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.send(draft) { draft = "" } },
                    enabled = draft.isNotBlank() && !state.isSending
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = t("Send"), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    pendingDelete?.let { comment ->
        ConfirmDialog(
            title = "Delete comment?",
            message = if (comment.userId == currentUserId) "Your comment will be removed." else "This comment will be removed for everyone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.delete(comment)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

@Composable
private fun CommentRow(comment: PostComment, isMine: Boolean, canDelete: Boolean, onDelete: () -> Unit) {
    val name = comment.authorName ?: t("Resident")
    Row(verticalAlignment = Alignment.Top) {
        // Initial in a circle, like a tiny avatar.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                name.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (isMine) t("%s (you)", name) else name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                RoleBadge(comment.authorRole)
            }
            Text(comment.body, style = MaterialTheme.typography.bodyMedium)
            Text(
                Dates.dateTime(comment.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = t("Delete comment"), modifier = Modifier.size(20.dp))
            }
        }
    }
}
