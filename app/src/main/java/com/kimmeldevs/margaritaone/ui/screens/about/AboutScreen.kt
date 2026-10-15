package com.kimmeldevs.margaritaone.ui.screens.about

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.kimmeldevs.margaritaone.data.model.Official
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.I18n
import com.kimmeldevs.margaritaone.i18n.t
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.painterResource
import com.kimmeldevs.margaritaone.R
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog
import com.kimmeldevs.margaritaone.ui.components.UiStateContent
import com.kimmeldevs.margaritaone.ui.viewmodel.AboutViewModel

private val TOWN_FACTS = listOf(
    "Founded" to "1892",
    "Barangays" to "36",
    "Population" to "27,012 (2024)",
    "Land area" to "129.12 km²"
)

/** About Santa Margarita: a short history, key facts and the municipal officials. Admins can edit the officials. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    canEdit: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = viewModel(factory = AboutViewModel.Factory)
) {
    val state by viewModel.officials.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.officials.isRefreshing.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val language = LocalLanguage.current

    // null = closed; Official(id = "") = adding a new one.
    var editing by remember { mutableStateOf<Official?>(null) }
    var confirmDelete by remember { mutableStateOf<Official?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(I18n.tr(language, it))
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(t("About Santa Margarita")) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                    }
                }
            )
        }
    ) { padding ->
        UiStateContent(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.officials.refresh() },
            modifier = Modifier.padding(padding)
        ) { officials ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.hero_town),
                    contentDescription = t("Aerial view of Santa Margarita"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(MaterialTheme.shapes.large)
                )
                Text(
                    t("Santa Margarita, Samar"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    t(
                        "Santa Margarita began as Magsohong, a barrio of Calbayog. Its leaders asked to become a town " +
                            "in 1878, and a royal decree in 1892 made it the town of Santa Margarita. Today it is a " +
                            "coastal municipality on the Samar Sea, in the first district of Samar."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TOWN_FACTS.forEach { (label, value) -> FactChip(t(label), value) }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t("Municipal officials"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(t("2025–2028 term"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    if (canEdit) {
                        TextButton(onClick = { editing = Official(id = "", name = "", position = "Councilor", sortOrder = 20) }) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Text(t("Add"), modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }

                val leaders = officials.filter { it.sortOrder < 10 }
                val council = officials.filter { it.sortOrder >= 10 }
                leaders.forEach { official ->
                    OfficialCard(
                        official = official,
                        photoUrl = viewModel.photoUrl(official),
                        large = true,
                        onClick = if (canEdit) ({ editing = official }) else null
                    )
                }
                if (council.isNotEmpty()) {
                    Text(
                        t("Sangguniang Bayan"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    council.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { official ->
                                OfficialCard(
                                    official = official,
                                    photoUrl = viewModel.photoUrl(official),
                                    large = false,
                                    onClick = if (canEdit) ({ editing = official }) else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Text(
                    t("Names are from public records. If something is wrong, please tell the Municipal Hall."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }

    editing?.let { official ->
        OfficialEditDialog(
            existing = official.takeIf { it.id.isNotEmpty() },
            initial = official,
            currentPhotoUrl = viewModel.photoUrl(official),
            isSaving = isSaving,
            onSave = { name, position, order, photo, removePhoto ->
                viewModel.save(official.takeIf { it.id.isNotEmpty() }, name, position, order, photo, removePhoto) {
                    editing = null
                }
            },
            onDelete = {
                editing = null
                confirmDelete = official
            },
            onDismiss = { editing = null }
        )
    }
    confirmDelete?.let { official ->
        ConfirmDialog(
            title = t("Remove %s?", official.name),
            message = t("They will no longer appear in the list of officials."),
            confirmLabel = t("Remove"),
            dismissLabel = t("Cancel"),
            destructive = true,
            onConfirm = {
                confirmDelete = null
                viewModel.delete(official)
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
private fun FactChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun OfficialCard(
    official: Official,
    photoUrl: String?,
    large: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (large) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                OfficialPhoto(official, photoUrl, size = 72.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(official.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(t(official.position), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (onClick != null) Icon(Icons.Filled.Edit, contentDescription = t("Edit"), tint = MaterialTheme.colorScheme.outline)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                OfficialPhoto(official, photoUrl, size = 56.dp)
                Text(
                    official.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(t(official.position), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

/** The official's photo, or their initials in a circle when there's none yet. */
@Composable
private fun OfficialPhoto(official: Official, photoUrl: String?, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            official.name.split(" ").filter { it.isNotBlank() && it.first().isLetter() }
                .let { listOfNotNull(it.firstOrNull(), it.lastOrNull()) }
                .joinToString("") { it.first().uppercase() },
            style = if (size > 60.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = t("Photo of %s", official.name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        }
    }
}

@Composable
private fun OfficialEditDialog(
    existing: Official?,
    initial: Official,
    currentPhotoUrl: String?,
    isSaving: Boolean,
    onSave: (name: String, position: String, sortOrder: Int, newPhoto: Uri?, removePhoto: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var position by rememberSaveable { mutableStateOf(initial.position) }
    var order by rememberSaveable { mutableStateOf(initial.sortOrder.toString()) }
    var newPhoto by remember { mutableStateOf<Uri?>(null) }
    var removePhoto by remember { mutableStateOf(false) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            newPhoto = uri
            removePhoto = false
        }
    }
    val shownPhoto: Any? = newPhoto ?: currentPhotoUrl.takeUnless { removePhoto }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(t(if (existing == null) "Add official" else "Edit official")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (shownPhoto != null) {
                            AsyncImage(model = shownPhoto, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp))
                        }
                    }
                    Column {
                        OutlinedButton(onClick = {
                            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text(t(if (shownPhoto == null) "Add photo" else "Change photo")) }
                        if (shownPhoto != null) {
                            TextButton(onClick = {
                                newPhoto = null
                                removePhoto = true
                            }) { Text(t("Remove photo")) }
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(t("Full name")) }, singleLine = true)
                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text(t("Position")) },
                    supportingText = { Text(t("e.g. Mayor, Vice Mayor, Councilor")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = order,
                    onValueChange = { value -> order = value.filter { it.isDigit() }.take(3) },
                    label = { Text(t("Order in list")) },
                    supportingText = { Text(t("1–9 shows at the top (mayor, vice mayor); 10 and up with the council.")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onSave(name, position, order.toIntOrNull() ?: 100, newPhoto, removePhoto) }
            ) { Text(t(if (isSaving) "Saving…" else "Save")) }
        },
        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(enabled = !isSaving, onClick = onDelete) {
                        Text(t("Remove"), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(enabled = !isSaving, onClick = onDismiss) { Text(t("Cancel")) }
            }
        }
    )
}
