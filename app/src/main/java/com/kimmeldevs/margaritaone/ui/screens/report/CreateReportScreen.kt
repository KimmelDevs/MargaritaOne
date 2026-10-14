package com.kimmeldevs.margaritaone.ui.screens.report

import com.kimmeldevs.margaritaone.i18n.t
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kimmeldevs.margaritaone.data.model.ReportCategory
import com.kimmeldevs.margaritaone.ui.components.BarangayPicker
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog
import com.kimmeldevs.margaritaone.ui.components.LocationPickerDialog
import android.widget.Toast
import androidx.compose.material3.TextButton
import com.kimmeldevs.margaritaone.ui.viewmodel.CreateReportViewModel
import com.kimmeldevs.margaritaone.util.LocationHelper
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReportScreen(
    onBackClick: () -> Unit,
    onSubmitted: (reportId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateReportViewModel = viewModel(factory = CreateReportViewModel.Factory)
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<ReportCategory?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmSubmit by remember { mutableStateOf(false) }
    var pickingOnMap by remember { mutableStateOf(false) }
    val offlineToast = t("No internet. Your report is saved and will be sent automatically when you're back online.")
    val hasDraft = title.isNotBlank() || description.isNotBlank() || address.isNotBlank() ||
        state.photoUri != null || category != null
    val tryLeave = { if (hasDraft && !state.isSubmitting) confirmDiscard = true else onBackClick() }

    BackHandler(enabled = hasDraft && state.submittedId == null && !state.savedOffline) { tryLeave() }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) viewModel.setPhoto(pendingCameraUri?.toUri())
        pendingCameraUri = null
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    val requestLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.fetchLocation()
    }

    LaunchedEffect(state.submittedId) {
        state.submittedId?.let(onSubmitted)
    }
    // No signal: the report is in the outbox and will send by itself (#10).
    LaunchedEffect(state.savedOffline) {
        if (state.savedOffline) {
            Toast.makeText(context, offlineToast, Toast.LENGTH_LONG).show()
            onBackClick()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(t("Report a problem")) },
                navigationIcon = {
                    IconButton(onClick = tryLeave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = category?.label?.let { t(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(t("Category")) },
                    placeholder = { Text(t("Choose a category")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ReportCategory.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(t(option.label)) },
                            onClick = {
                                category = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                label = { Text(t("Title")) },
                placeholder = { Text(t("Pothole near barangay hall")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(2000) },
                label = { Text(t("Description")) },
                placeholder = { Text(t("Describe the issue in detail")) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )

            // Photo
            Text(t("Photo (optional)"), style = MaterialTheme.typography.labelLarge)
            if (state.photoUri != null) {
                Box {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = t("Selected photo"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    FilledTonalIconButton(
                        onClick = { viewModel.setPhoto(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = t("Remove photo"))
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val uri = newCameraUri(context)
                            pendingCameraUri = uri.toString()
                            try {
                                takePicture.launch(uri)
                            } catch (_: ActivityNotFoundException) {
                                pendingCameraUri = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Text(t("Camera"), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Text(t("Gallery"), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            // Location
            Text(t("Location"), style = MaterialTheme.typography.labelLarge)
            val location = state.location
            if (location != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = viewModel::clearLocation) {
                        Icon(Icons.Filled.Close, contentDescription = t("Remove location"))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (LocationHelper.hasPermission(context)) {
                            viewModel.fetchLocation()
                        } else {
                            requestLocation.launch(LocationHelper.permissions)
                        }
                    },
                    enabled = !state.isLocating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLocating) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text(t("Getting location…"), modifier = Modifier.padding(start = 8.dp))
                    } else {
                        Icon(Icons.Filled.LocationOn, contentDescription = null)
                        Text(t("Add my current location"), modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            // Choose on a map (#9): when GPS is off/inaccurate, or the problem is somewhere else.
            TextButton(onClick = { pickingOnMap = true }) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Text(
                    t(if (location == null) "Choose on map instead" else "Adjust on map"),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            BarangayPicker(
                barangays = state.barangays,
                selectedName = state.barangay?.name.orEmpty(),
                onSelect = viewModel::setBarangay,
                onFreeText = {},
                label = "Barangay"
            )
            OutlinedTextField(
                value = address,
                onValueChange = { address = it.take(200) },
                label = { Text(t(if (state.barangays.isEmpty()) "Barangay / landmark" else "Landmark (optional)")) },
                placeholder = { Text(t("e.g. Brgy. Rizal, near the covered court")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(t(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    if (viewModel.validate(title, category)) confirmSubmit = true
                },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text(t("Sending…"), modifier = Modifier.padding(start = 8.dp))
                } else {
                    Text(t("Submit report"))
                }
            }
        }
    }

    if (pickingOnMap) {
        LocationPickerDialog(
            initial = state.location,
            onPicked = {
                viewModel.setLocation(it)
                pickingOnMap = false
            },
            onDismiss = { pickingOnMap = false }
        )
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = t("Discard this report?"),
            message = t("What you've typed and the photo you attached will be lost."),
            confirmLabel = t("Discard"),
            dismissLabel = t("Keep editing"),
            destructive = true,
            onConfirm = {
                confirmDiscard = false
                onBackClick()
            },
            onDismiss = { confirmDiscard = false }
        )
    }
    if (confirmSubmit) {
        val summary = buildString {
            appendLine(t("Category: %s", t(category?.label.orEmpty())))
            appendLine(t("Title: %s", title.trim()))
            appendLine(t(if (state.photoUri != null) "Photo: attached" else "Photo: none"))
            appendLine(t(if (state.location != null) "GPS location: attached" else "GPS location: not attached"))
            state.barangay?.let { appendLine(t("Barangay: %s", it.name)) }
            if (address.isNotBlank()) append(t("Place: %s", address.trim()))
        }.trim()
        ConfirmDialog(
            title = t("Send this report?"),
            message = summary,
            confirmLabel = t("Send"),
            dismissLabel = t("Edit"),
            onConfirm = {
                confirmSubmit = false
                viewModel.submit(title, description, category, address)
            },
            onDismiss = { confirmSubmit = false }
        )
    }
}

private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "report_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
