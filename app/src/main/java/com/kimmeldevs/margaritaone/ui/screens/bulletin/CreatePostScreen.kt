package com.kimmeldevs.margaritaone.ui.screens.bulletin

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kimmeldevs.margaritaone.data.model.PostType
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog
import com.kimmeldevs.margaritaone.ui.components.DateTimeField
import com.kimmeldevs.margaritaone.ui.components.LoadingBox
import com.kimmeldevs.margaritaone.ui.viewmodel.CreatePostViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.MAX_POST_IMAGES
import com.kimmeldevs.margaritaone.ui.viewmodel.hasUnsavedChanges

/** New bulletin post, or edit an existing one (#3), with photos, PDF, scheduling (#18), and event details (#12). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onBackClick: () -> Unit,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePostViewModel = viewModel(factory = CreatePostViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form = state.form
    val isEdit = state.editing != null
    var confirmDiscard by remember { mutableStateOf(false) }
    val unsaved = state.hasUnsavedChanges
    val tryLeave = { if (unsaved && !state.isSubmitting) confirmDiscard = true else onBackClick() }

    BackHandler(enabled = unsaved && !state.posted) { tryLeave() }

    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.setDocument(uri)
    }
    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_POST_IMAGES)
    ) { uris -> if (uris.isNotEmpty()) viewModel.addImages(uris) }

    LaunchedEffect(state.posted) {
        if (state.posted) onPosted()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(t(if (isEdit) "Edit post" else "New bulletin post")) },
                navigationIcon = {
                    IconButton(onClick = tryLeave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(t("Type"), style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                PostType.entries.forEach { option ->
                    FilterChip(
                        selected = form.type == option,
                        onClick = { viewModel.update { it.copy(type = option) } },
                        label = { Text(t(option.label)) }
                    )
                }
            }
            if (form.type == PostType.EMERGENCY) {
                Text(
                    t("Emergency posts are highlighted in red at the top of everyone's bulletin when pinned."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = form.title,
                onValueChange = { v -> viewModel.update { it.copy(title = v.take(160)) } },
                label = { Text(t("Title")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.body,
                onValueChange = { v -> viewModel.update { it.copy(body = v) } },
                label = { Text(t("Message")) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            // Event details (#12)
            if (form.type == PostType.EVENT) {
                Text(t("Event details"), style = MaterialTheme.typography.labelLarge)
                DateTimeField(
                    millis = form.eventStartMillis,
                    placeholder = "Starts",
                    onChange = { v -> viewModel.update { it.copy(eventStartMillis = v) } }
                )
                DateTimeField(
                    millis = form.eventEndMillis,
                    placeholder = "Ends (optional)",
                    onChange = { v -> viewModel.update { it.copy(eventEndMillis = v) } }
                )
                OutlinedTextField(
                    value = form.eventLocation,
                    onValueChange = { v -> viewModel.update { it.copy(eventLocation = v.take(200)) } },
                    label = { Text(t("Where")) },
                    placeholder = { Text(t("e.g. Municipal covered court")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("Pin to top"), modifier = Modifier.weight(1f))
                Switch(checked = form.pinned, onCheckedChange = { v -> viewModel.update { it.copy(pinned = v) } })
            }

            // Photos (kept existing ones first, then newly picked)
            if (form.photoCount > 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(form.existingImagePaths, key = { it }) { path ->
                        PhotoThumb(
                            model = viewModel.imageUrl(path),
                            onRemove = { viewModel.update { f -> f.copy(existingImagePaths = f.existingImagePaths - path) } }
                        )
                    }
                    items(form.newImageUris, key = { it.toString() }) { uri ->
                        PhotoThumb(
                            model = uri,
                            onRemove = { viewModel.update { f -> f.copy(newImageUris = f.newImageUris - uri) } }
                        )
                    }
                }
            }
            if (form.photoCount < MAX_POST_IMAGES) {
                OutlinedButton(
                    onClick = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text(
                        t("Add photos (%1\$d/%2\$d)", form.photoCount, MAX_POST_IMAGES),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            // PDF
            val documentLabel = form.newDocumentName ?: form.existingDocumentPath?.let { t("Attached PDF") }
            if (documentLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                    Text(
                        documentLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                    )
                    IconButton(onClick = {
                        if (form.newDocumentUri != null) viewModel.setDocument(null) else viewModel.removeExistingDocument()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = t("Remove attachment"))
                    }
                }
            } else {
                OutlinedButton(onClick = { pickDocument.launch(arrayOf("application/pdf")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                    Text(t("Attach a PDF (optional)"), modifier = Modifier.padding(start = 6.dp))
                }
            }

            HorizontalDivider()

            // Schedule (#18)
            Text(t("Publish"), style = MaterialTheme.typography.labelLarge)
            DateTimeField(
                millis = form.scheduledAtMillis,
                placeholder = if (isEdit) "Keep the current time" else "Now",
                onChange = { v -> viewModel.update { it.copy(scheduledAtMillis = v) } }
            )
            if (form.scheduledAtMillis != null && form.scheduledAtMillis > System.currentTimeMillis()) {
                Text(
                    t("Residents will see this post at the scheduled time. Until then only admins can see it."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.error?.let {
                Text(t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text(t("Saving…"), modifier = Modifier.padding(start = 8.dp))
                } else {
                    val scheduled = form.scheduledAtMillis != null && form.scheduledAtMillis > System.currentTimeMillis()
                    Text(t(if (isEdit) "Save changes" else if (scheduled) "Schedule" else "Publish"))
                }
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = if (isEdit) "Discard changes?" else "Discard this post?",
            message = "Your draft will be lost.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            destructive = true,
            onConfirm = {
                confirmDiscard = false
                onBackClick()
            },
            onDismiss = { confirmDiscard = false }
        )
    }
}

@Composable
private fun PhotoThumb(model: Any, onRemove: () -> Unit) {
    Box {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        FilledTonalIconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(2.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = t("Remove photo"), modifier = Modifier.size(16.dp))
        }
    }
}
