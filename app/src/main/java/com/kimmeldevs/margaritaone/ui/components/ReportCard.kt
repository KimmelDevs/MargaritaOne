package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.AddRoad
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kimmeldevs.margaritaone.data.model.ReportCategory
import com.kimmeldevs.margaritaone.data.model.ReportDto
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.util.Dates

fun ReportCategory.icon(): ImageVector = when (this) {
    ReportCategory.ROAD -> Icons.Outlined.AddRoad
    ReportCategory.GARBAGE -> Icons.Outlined.Delete
    ReportCategory.STREETLIGHT -> Icons.Outlined.Lightbulb
    ReportCategory.PEACE_ORDER -> Icons.Outlined.LocalPolice
    ReportCategory.OTHER -> Icons.Outlined.HelpOutline
}

fun ReportCategory.tint(): Color = when (this) {
    ReportCategory.ROAD -> Color(0xFF0866C6)
    ReportCategory.GARBAGE -> Color(0xFF0F9F6E)
    ReportCategory.STREETLIGHT -> Color(0xFFB7791F)
    ReportCategory.PEACE_ORDER -> Color(0xFF0D3157)
    ReportCategory.OTHER -> Color(0xFF64748B)
}

/**
 * Where a report stands as "Step x of n". Staff see 4 steps, including whether anyone at the LGU has opened it;
 * residents see 3 and are never told whether staff opened their report (that stays staff-only).
 */
private data class ReportStep(val done: Int, val total: Int, val text: String)

private fun stepFor(report: ReportDto, staffView: Boolean, seen: Boolean): ReportStep = if (staffView) {
    when (report.status) {
        ReportStatus.PENDING -> if (seen) ReportStep(2, 4, "Seen by LGU") else ReportStep(1, 4, "Waiting for review")
        ReportStatus.IN_PROGRESS ->
            ReportStep(3, 4, if (report.assignedTo != null) "Assigned to staff" else "Being worked on")
        ReportStatus.RESOLVED -> ReportStep(4, 4, "Resolved")
        ReportStatus.REJECTED -> ReportStep(0, 4, "Rejected")
        ReportStatus.CANCELLED -> ReportStep(0, 4, "Cancelled by the resident")
    }
} else {
    when (report.status) {
        ReportStatus.PENDING -> ReportStep(1, 3, "Submitted, waiting for review")
        ReportStatus.IN_PROGRESS -> ReportStep(2, 3, "The LGU is working on it")
        ReportStatus.RESOLVED -> ReportStep(3, 3, "Resolved")
        ReportStatus.REJECTED -> ReportStep(0, 3, "Rejected")
        ReportStatus.CANCELLED -> ReportStep(0, 3, "Cancelled")
    }
}

@Composable
fun ReportCard(
    report: ReportDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    isUpdated: Boolean = false,
    /** A staff member or admin has opened it (only known to staff). */
    isSeen: Boolean = false,
    /** Staff dashboard: 4-step progress including "Seen by LGU". */
    staffView: Boolean = false
) {
    val step = stepFor(report, staffView, isSeen)
    val color = statusColor(report.status)
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category icon, or the report's photo when there is one.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(report.category.tint().copy(alpha = 0.12f))
                ) {
                    if (thumbnailUrl != null) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(52.dp)
                        )
                    } else {
                        Icon(report.category.icon(), contentDescription = null, tint = report.category.tint(), modifier = Modifier.size(26.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isUpdated) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Text(
                            report.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isUpdated) FontWeight.ExtraBold else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        listOfNotNull(t(report.category.label), Dates.date(report.createdAt)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(status = report.status)
            }
            StepProgress(done = step.done, total = step.total, color = color)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildString {
                        if (step.done > 0) append(t("Step %1\$d of %2\$d", step.done, step.total)).append(" · ")
                        append(t(step.text))
                        if (isUpdated) append(" · ").append(t("Updated"))
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (staffView && isSeen) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                    Text(
                        t("Seen"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            report.referenceNumber?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
