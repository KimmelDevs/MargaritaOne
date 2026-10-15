package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kimmeldevs.margaritaone.data.model.ReactionType
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.viewmodel.ReactionSummary

/**
 * Facebook-style reactions under a post:
 * a summary line (emoji stack + count, tap for the breakdown), then Like / Share buttons.
 * Tap Like to like or remove your reaction; long-press it to pick Love, Care, Haha, Wow, Sad, or Angry.
 */
@Composable
fun ReactionBar(
    summary: ReactionSummary,
    commentCount: Int,
    onReact: (ReactionType) -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBreakdown by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (summary.total > 0 || commentCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (summary.total > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showBreakdown = true }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        EmojiStack(summary.top)
                        Text(
                            text = reactionCountText(summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                Box(Modifier.weight(1f))
                if (commentCount > 0) {
                    Text(
                        text = t(if (commentCount == 1) "1 comment" else "%d comments", commentCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onComment)
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReactButton(mine = summary.mine, onReact = onReact, modifier = Modifier.weight(1f))
            ActionButton(onClick = onComment, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                ActionLabel(t("Comment"))
            }
            ActionButton(
                onClick = onShare,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                ActionLabel(t("Share"))
            }
        }
    }

    if (showBreakdown) {
        ReactionBreakdownSheet(summary = summary, onDismiss = { showBreakdown = false })
    }
}

@Composable
private fun reactionCountText(summary: ReactionSummary): String {
    val others = summary.total - 1
    return when {
        summary.mine == null -> summary.total.toString()
        others == 0 -> t("You")
        else -> t("You and %d others", others)
    }
}

/** Overlapping circles of the top reactions, like Facebook's. */
@Composable
private fun EmojiStack(top: List<ReactionType>) {
    Box {
        top.forEachIndexed { i, type ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = (i * 14).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.5.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Text(type.emoji, fontSize = 12.sp)
            }
        }
        // Reserve the width the offset circles occupy.
        Box(Modifier.size(width = (20 + (top.size - 1).coerceAtLeast(0) * 14).dp, height = 20.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReactButton(
    mine: ReactionType?,
    onReact: (ReactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val color = when (mine) {
        null -> MaterialTheme.colorScheme.onSurfaceVariant
        ReactionType.LIKE -> MaterialTheme.colorScheme.secondary
        ReactionType.LOVE, ReactionType.ANGRY -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .combinedClickable(
                    // Tap: like, or remove whatever reaction you have.
                    onClick = { onReact(mine ?: ReactionType.LIKE) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        pickerOpen = true
                    }
                )
                .padding(vertical = 10.dp)
        ) {
            if (mine == null) {
                Icon(Icons.Outlined.ThumbUp, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            } else {
                Text(mine.emoji, fontSize = 16.sp)
            }
            ActionLabel(t(mine?.label ?: "Like"), color = color)
        }

        if (pickerOpen) {
            ReactionPicker(
                onPick = { type ->
                    pickerOpen = false
                    // Picking your current reaction again removes it; the ViewModel handles that toggle.
                    onReact(type)
                },
                onDismiss = { pickerOpen = false }
            )
        }
    }
}

/** The floating row of 7 emoji that appears on long-press. */
@Composable
private fun ReactionPicker(onPick: (ReactionType) -> Unit, onDismiss: () -> Unit) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, -170),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 8.dp,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ReactionType.entries.forEach { type ->
                    PickerEmoji(type, onPick)
                }
            }
        }
    }
}

@Composable
private fun PickerEmoji(type: ReactionType, onPick: (ReactionType) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 1.4f else 1f, label = "reactionScale")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            type.emoji,
            fontSize = 28.sp,
            modifier = Modifier
                .scale(scale)
                .clip(CircleShape)
                .clickable(interactionSource = interaction, indication = null) { onPick(type) }
                .padding(4.dp)
        )
    }
}

@Composable
private fun ActionButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) { content() }
    }
}

/** Bottom sheet with how many of each reaction a post got. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReactionBreakdownSheet(summary: ReactionSummary, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Text(
                t("Reactions (%d)", summary.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ReactionType.entries
                .filter { (summary.counts[it] ?: 0) > 0 }
                .sortedByDescending { summary.counts[it] }
                .forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(type.emoji, fontSize = 22.sp)
                        Text(
                            t(type.label),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                        Text(
                            (summary.counts[type] ?: 0).toString(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (summary.mine == type) MaterialTheme.colorScheme.primary else Color.Unspecified
                        )
                    }
                }
        }
    }
}

/** Button label that always stays on one line (shrinks a little instead of wrapping "Comme / nt"). */
@Composable
private fun ActionLabel(text: String, color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        softWrap = false,
        overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
        modifier = Modifier.padding(start = 5.dp)
    )
}

