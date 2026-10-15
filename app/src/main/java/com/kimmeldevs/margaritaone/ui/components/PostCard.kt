package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.kimmeldevs.margaritaone.ui.theme.BrandGradient
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
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        // Emergencies keep a red outline so they stand out in the feed.
        border = if (isEmergency) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isNew && !isScheduled) {
                    PillBadge(text = "New", containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                }
                if (isScheduled && publishMillis != null) {
                    PillBadge(
                        text = t("Scheduled · %s", Dates.formatMillis(publishMillis, "MMM d, h:mm a")),
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                }
                PillBadge(
                    text = t(post.type.label).uppercase(),
                    containerColor = if (isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isEmergency) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (post.pinned) PillBadge(text = "Pinned")
                Text(
                    text = Dates.date(post.publishedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (onEdit != null || onDelete != null) {
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = t("More options"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            onEdit?.let { edit ->
                                DropdownMenuItem(
                                    text = { Text(t("Edit post")) },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = { menuOpen = false; edit() }
                                )
                            }
                            onDelete?.let { delete ->
                                DropdownMenuItem(
                                    text = { Text(t("Delete post"), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; delete() }
                                )
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(end = 8.dp)) {
                // Who posted it (accountability).
                post.authorName?.let { author ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandGradient)
                        ) {
                            Text(author.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(author, style = MaterialTheme.typography.titleSmall)
                        RoleBadge(post.authorRole)
                    }
                }
                post.editedByName?.let { editor ->
                    Text(
                        text = t("Edited by %s", editor) + (post.editedAt?.let { " · " + Dates.dateTime(it) } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
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
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clip(MaterialTheme.shapes.large)
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
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
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
