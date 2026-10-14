package com.kimmeldevs.margaritaone.ui.screens.report

import com.kimmeldevs.margaritaone.i18n.I18n
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.t
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.data.model.ReportUpdateDto
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog
import com.kimmeldevs.margaritaone.ui.components.RefreshOnResume
import com.kimmeldevs.margaritaone.ui.components.StatusBadge
import com.kimmeldevs.margaritaone.ui.components.UiStateContent
import com.kimmeldevs.margaritaone.ui.components.ZoomableImageDialog
import com.kimmeldevs.margaritaone.ui.viewmodel.ActionState
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportDetail
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportDetailViewModel
import com.kimmeldevs.margaritaone.util.Dates
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    isStaff: Boolean,
    currentUserId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportDetailViewModel = viewModel(factory = ReportDetailViewModel.factory(isStaff, currentUserId))
) {
    val state by viewModel.detail.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.detail.isRefreshing.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val language = LocalLanguage.current

    LaunchedEffect(actionState.message) {
        actionState.message?.let {
            snackbarHostState.showSnackbar(I18n.tr(language, it))
            viewModel.clearMessage()
        }
    }

    RefreshOnResume { viewModel.detail.refreshIfLoaded() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(t("Report details")) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        UiStateContent(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.detail.refresh() },
            modifier = Modifier.padding(innerPadding)
        ) { detail ->
            ReportDetailContent(
                detail = detail,
                isStaff = isStaff,
                currentUserId = currentUserId,
                actionState = actionState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun ReportDetailContent(
    detail: ReportDetail,
    isStaff: Boolean,
    currentUserId: String,
    actionState: ActionState,
    viewModel: ReportDetailViewModel
) {
    val report = detail.report
    val context = LocalContext.current
    val language = LocalLanguage.current
    var showPhoto by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Photo; tap for full screen (#17)
        if (detail.photoUrl != null) {
            AsyncImage(
                model = detail.photoUrl,
                contentDescription = t("Photo of the reported problem. Tap to enlarge."),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showPhoto = true }
            )
            TextButton(onClick = { showPhoto = true }) {
                Icon(Icons.Filled.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(t("View full photo"), modifier = Modifier.padding(start = 4.dp))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            StatusBadge(status = report.status)
            Text(
                text = t("Submitted %s", Dates.dateTime(report.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Text(
            text = report.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (report.description.isNotBlank()) {
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            // Reference number with copy (#19)
            report.referenceNumber?.let { ref ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DetailRow(t("Reference no."), ref, Modifier.weight(1f))
                    IconButton(onClick = { copyToClipboard(context, ref, I18n.tr(language, "Copied %s", ref)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = t("Copy reference number"))
                    }
                }
            }
            DetailRow(t("Category"), t(report.category.label))
            detail.barangayName?.let { DetailRow(t("Barangay"), it) }
            report.address?.takeIf { it.isNotBlank() }?.let { DetailRow(t("Location"), it) }
            detail.reporter?.let { reporter ->
                DetailRow(t("Reported by"), reporter.displayName)
                reporter.phone?.let { DetailRow(t("Mobile"), it) }
                reporter.email?.let { DetailRow(t("Email"), it) }
            }
            if (isStaff) {
                DetailRow(
                    t("Assigned to"),
                    when {
                        report.assignedTo == null -> t("Nobody yet")
                        report.assignedTo == currentUserId -> t("You")
                        else -> detail.assignee?.displayName ?: t("Another staff member")
                    }
                )
            }
        }

        if (report.latitude != null && report.longitude != null) {
            val lat = report.latitude
            val lng = report.longitude
            TextButton(onClick = { openInMaps(context, lat, lng) }) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Text(t("Open location in maps"), modifier = Modifier.padding(start = 6.dp))
            }
        }

        // "Was this fixed?" (#11): the reporter rates once; staff see the answer.
        val isReporter = report.reporterId == currentUserId
        if (report.status == ReportStatus.RESOLVED) {
            if (isReporter && report.resolutionRating == null) {
                RateResolutionCard(isSaving = actionState.isSaving, onRate = viewModel::rateResolution)
            } else if (report.resolutionRating != null) {
                ResolutionRatingRow(
                    rating = report.resolutionRating,
                    feedback = report.resolutionFeedback,
                    byMe = isReporter
                )
            }
        }

        // Cancel my report (#18)
        if (!isStaff && report.reporterId == currentUserId && report.status == ReportStatus.PENDING) {
            OutlinedButton(
                onClick = { confirmCancel = true },
                enabled = !actionState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(t("Cancel this report"), color = MaterialTheme.colorScheme.error)
            }
        }

        if (isStaff) {
            StaffPanel(
                current = report.status,
                isAssignedToMe = report.assignedTo == currentUserId,
                actionState = actionState,
                viewModel = viewModel
            )
        }

        Text(
            text = t("Status timeline"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            detail.timeline.forEachIndexed { index, update ->
                TimelineRow(update = update, isLatest = index == detail.timeline.lastIndex)
            }
        }
    }

    if (showPhoto && detail.photoUrl != null) {
        ZoomableImageDialog(url = detail.photoUrl, contentDescription = report.title, onDismiss = { showPhoto = false })
    }
    if (confirmCancel) {
        ConfirmDialog(
            title = t("Cancel this report?"),
            message = t("The LGU will stop working on \"%s\". You can't undo this.", report.title),
            confirmLabel = t("Cancel report"),
            dismissLabel = t("Keep it"),
            destructive = true,
            onConfirm = {
                confirmCancel = false
                viewModel.cancelReport()
            },
            onDismiss = { confirmCancel = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffPanel(
    current: ReportStatus,
    isAssignedToMe: Boolean,
    actionState: ActionState,
    viewModel: ReportDetailViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var newStatus by rememberSaveable { mutableStateOf<ReportStatus?>(null) }
    var note by rememberSaveable { mutableStateOf("") }
    val clear = { newStatus = null; note = "" }
    val closed = current == ReportStatus.RESOLVED || current == ReportStatus.REJECTED || current == ReportStatus.CANCELLED

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(t("Staff actions"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

            // Assign to me (#21)
            OutlinedButton(
                onClick = { viewModel.assignToMe(!isAssignedToMe) },
                enabled = !actionState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(t(if (isAssignedToMe) "Unassign me" else "Assign to me"))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(t("Note to the reporter (optional)")) },
                placeholder = { Text(t("e.g. Crew scheduled for Monday")) },
                modifier = Modifier.fillMaxWidth()
            )

            // Quick status buttons (#20); the note above is sent along with them.
            if (!closed) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    if (current == ReportStatus.PENDING) {
                        FilledTonalButton(
                            onClick = { viewModel.addUpdate(ReportStatus.IN_PROGRESS, note, clear) },
                            enabled = !actionState.isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(t("Start"), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    Button(
                        onClick = { viewModel.addUpdate(ReportStatus.RESOLVED, note, clear) },
                        enabled = !actionState.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(t("Resolve"), modifier = Modifier.padding(start = 4.dp))
                    }
                    OutlinedButton(
                        onClick = { viewModel.addUpdate(ReportStatus.REJECTED, note, clear) },
                        enabled = !actionState.isSaving,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(t("Reject"), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            // Full control: any status, or just a note.
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = newStatus?.label?.let { t(it) } ?: t("Keep as %s", t(current.label)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(t("Other status")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(t("Keep as %s", t(current.label))) },
                        onClick = { newStatus = null; expanded = false }
                    )
                    ReportStatus.entries.filter { it != current && it.isStaffSettable }.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(t(status.label)) },
                            onClick = { newStatus = status; expanded = false }
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { viewModel.addUpdate(newStatus, note, clear) },
                enabled = !actionState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(t(if (actionState.isSaving) "Saving…" else if (newStatus == null) "Post note only" else "Post update"))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TimelineRow(update: ReportUpdateDto, isLatest: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val done = !isLatest || update.status == ReportStatus.RESOLVED
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Column {
            Text(
                text = t(update.status?.label ?: "Note"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            update.note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                text = Dates.dateTime(update.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String, toast: String) {
    context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("Reference number", text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

private fun openInMaps(context: Context, lat: Double, lng: Double) {
    val coords = String.format(Locale.US, "%.6f,%.6f", lat, lng)
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, "geo:$coords?q=$coords".toUri()))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/search/?api=1&query=$coords".toUri()))
    }
}

@Composable
private fun RateResolutionCard(isSaving: Boolean, onRate: (fixed: Boolean, feedback: String) -> Unit) {
    var feedback by rememberSaveable { mutableStateOf("") }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t("Was this fixed?"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                t("Your answer helps the LGU know if the problem is really solved."),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it.take(500) },
                placeholder = { Text(t("Anything to add? (optional)")) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onRate(true, feedback) }, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text("👍 " + t("Yes, fixed"))
                }
                OutlinedButton(onClick = { onRate(false, feedback) }, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text("👎 " + t("Not fixed"))
                }
            }
        }
    }
}

@Composable
private fun ResolutionRatingRow(rating: Int, feedback: String?, byMe: Boolean) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        val answer = t(if (rating > 0) "Fixed" else "Not fixed")
        Text(
            (if (rating > 0) "👍 " else "👎 ") + t(if (byMe) "You said: %s" else "Resident said: %s", answer),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (rating > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        feedback?.let {
            Text("\u201C$it\u201D", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
