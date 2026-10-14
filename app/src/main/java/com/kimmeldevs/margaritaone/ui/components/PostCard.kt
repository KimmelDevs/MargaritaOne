package com.kimmeldevs.margaritaone.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.data.model.PostDto
import com.kimmeldevs.margaritaone.data.model.PostType
import com.kimmeldevs.margaritaone.data.model.ReactionType
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.viewmodel.ReactionSummary
import com.kimmeldevs.margaritaone.util.Dates

@Composable
fun PostCard(
    post: PostDto,
    imageUrls: List<String>,
    documentUrl: String?,
    reactions: ReactionSummary,
    commentCount: Int,
    onComment: () -> Unit,
    onOpenDocument: (String) -> Unit,
    onViewImage: (index: Int) -> Unit,
    onReact: (ReactionType) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val publishMillis = Dates.toMillis(post.publishedAt)
    val isScheduled = publishMillis != null && publishMillis > System.currentTimeMillis()
    val isEmergency = post.type == PostType.EMERGENCY
    val isNew = Dates.isWithinHours(post.publishedAt, 24)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmergency) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isScheduled && publishMillis != null) {
                    PillBadge(
                        text = t("Scheduled · %s", Dates.formatMillis(publishMillis, "MMM d, h:mm a")),
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                }
                if (post.pinned) PillBadge(text = "Pinned")
                // "New" badge (#13)
                if (isNew && !isScheduled) {
                    PillBadge(
                        text = "New",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
                if (isEmergency) {
                    PillBadge(
                        text = post.type.label,
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    PillBadge(
                        text = post.type.label,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = Dates.date(post.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = t("Edit post"))
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = t("Delete post"))
                    }
                }
            }

            post.authorName?.let { author ->
                AuthorLine(
                    text = t("Posted by %s", author),
                    role = post.authorRole,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            post.editedByName?.let { editor ->
                Text(
                    text = t("Edited by %s", editor) + (post.editedAt?.let { " · " + Dates.dateTime(it) } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = post.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            if (post.body.isNotBlank()) {
                Text(
                    text = post.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (post.type == PostType.EVENT && post.eventStart != null) {
                EventBox(post = post, modifier = Modifier.padding(top = 8.dp))
            }

            // Photos, Facebook-style; tap opens the swipeable viewer.
            PostImageGrid(
                urls = imageUrls,
                onImageClick = onViewImage,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (documentUrl != null) {
                TextButton(onClick = { onOpenDocument(documentUrl) }) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Text(t("View attachment"), modifier = Modifier.padding(start = 6.dp))
                }
            }

            ReactionBar(
                summary = reactions,
                commentCount = commentCount,
                onReact = onReact,
                onComment = onComment,
                onShare = onShare,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** When and where an event happens, with "Add to calendar" (#12). */
@Composable
private fun EventBox(post: PostDto, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val start = post.eventStart?.let(Dates::toMillis) ?: return
    val end = post.eventEnd?.let(Dates::toMillis)
    val ended = (end ?: start) < System.currentTimeMillis()
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
        ) {
            Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    buildString {
                        append(Dates.formatMillis(start))
                        if (end != null) append(" – ").append(Dates.formatMillis(end, "h:mm a"))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                post.eventLocation?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                if (ended) {
                    Text(t("This event has ended."), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            if (!ended) {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end ?: (start + 60 * 60 * 1000))
                        .putExtra(CalendarContract.Events.TITLE, post.title)
                        .putExtra(CalendarContract.Events.DESCRIPTION, post.body)
                        .putExtra(CalendarContract.Events.EVENT_LOCATION, post.eventLocation)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        // No calendar app installed.
                    }
                }) {
                    Text(t("Add to calendar"))
                }
            }
        }
    }
}
