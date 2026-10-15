package com.kimmeldevs.margaritaone.ui.screens.hotlines

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kimmeldevs.margaritaone.ui.components.IconBadge
import com.kimmeldevs.margaritaone.ui.theme.PrimaryRed
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.data.model.EmergencyContact
import com.kimmeldevs.margaritaone.data.model.EmergencyContactInput
import com.kimmeldevs.margaritaone.data.model.HotlineCategory
import com.kimmeldevs.margaritaone.data.model.HotlineIcon
import com.kimmeldevs.margaritaone.i18n.I18n
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog
import com.kimmeldevs.margaritaone.ui.components.EmptyListText
import com.kimmeldevs.margaritaone.ui.components.ListSectionHeader
import com.kimmeldevs.margaritaone.ui.components.RefreshOnResume
import com.kimmeldevs.margaritaone.ui.components.SkeletonList
import com.kimmeldevs.margaritaone.ui.viewmodel.HotlinesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotlinesScreen(
    canEdit: Boolean,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    viewModel: HotlinesViewModel = viewModel(factory = HotlinesViewModel.Factory)
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val language = LocalLanguage.current
    val context = LocalContext.current
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<EmergencyContact?>(null) }
    var managingCategories by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(I18n.tr(language, it))
            viewModel.clearMessage()
        }
    }

    RefreshOnResume { viewModel.refresh(showIndicator = false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(t("Emergency hotlines")) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                        }
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { managingCategories = true }) {
                            Icon(Icons.Filled.Category, contentDescription = t("Manage categories"))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canEdit) {
                ExtendedFloatingActionButton(
                    onClick = { editing = EditTarget(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(t("Add hotline")) }
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.isLoading) {
                SkeletonList()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp)
                ) {
                    item {
                        Text(
                            t("Tap a hotline to call or copy its number. Numbers are saved on your phone, so they still show without internet."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.loadError != null && state.contacts.isNotEmpty()) {
                        item {
                            Text(
                                t("Couldn't refresh. Showing the numbers saved on this phone."),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (state.contacts.isEmpty()) {
                        item {
                            EmptyListText(
                                state.loadError ?: if (canEdit) {
                                    "No hotlines yet. Tap \"Add hotline\" to add the local numbers."
                                } else {
                                    "No hotlines yet."
                                }
                            )
                        }
                    }
                    viewModel.grouped(state).forEach { (category, contacts) ->
                        item(key = "header_${category?.id ?: "none"}") {
                            ListSectionHeader(category?.name ?: "Other")
                        }
                        items(contacts, key = { it.id }) { contact ->
                            HotlineCard(
                                contact = contact,
                                category = category,
                                expanded = expandedId == contact.id,
                                onToggle = { expandedId = if (expandedId == contact.id) null else contact.id },
                                onCall = { dial(context, contact.phone) },
                                onCopy = {
                                    copyNumber(context, contact.phone, I18n.tr(language, "Copied %s", contact.phone))
                                },
                                canEdit = canEdit,
                                onEdit = { editing = EditTarget(contact) },
                                onDelete = { pendingDelete = contact }
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { target ->
        HotlineEditDialog(
            existing = target.contact,
            categories = state.categories,
            isSaving = state.isSaving,
            onSave = { input -> viewModel.saveContact(target.contact, input) { editing = null } },
            onDismiss = { editing = null }
        )
    }
    pendingDelete?.let { contact ->
        ConfirmDialog(
            title = "Delete hotline?",
            message = t("\"%s\" will be removed for everyone.", contact.name),
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteContact(contact)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
    if (managingCategories) {
        HotlineCategoriesSheet(
            categories = state.categories,
            contacts = state.contacts,
            isSaving = state.isSaving,
            onSave = viewModel::saveCategory,
            onDelete = viewModel::deleteCategory,
            onDismiss = { managingCategories = false }
        )
    }
}

/** Wrapper so "add new" (null contact) is distinguishable from "no dialog". */
private data class EditTarget(val contact: EmergencyContact?)

/**
 * Collapsed: name, number, icon. Tap to expand and reveal Call / Copy number — the call button only
 * appears after a deliberate tap, and the phone's dialer is a second step, so pocket-calls can't happen.
 */
@Composable
private fun HotlineCard(
    contact: EmergencyContact,
    category: HotlineCategory?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCall: () -> Unit,
    onCopy: () -> Unit,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val urgent = category?.isUrgent == true
    val onCard = if (urgent) Color.White else MaterialTheme.colorScheme.onSurface
    val subtle = if (urgent) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        onClick = onToggle,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = if (urgent) Color.Transparent else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (urgent) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .then(if (urgent) Modifier.background(Brush.linearGradient(listOf(PrimaryRed, Color(0xFFC8102E)))) else Modifier)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (urgent) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(HotlineIcon.fromKey(category?.icon).vector(), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                } else {
                    IconBadge(HotlineIcon.fromKey(category?.icon).vector(), tint = MaterialTheme.colorScheme.primary)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(contact.name, style = MaterialTheme.typography.titleSmall, color = onCard)
                    Text(
                        contact.phone,
                        style = if (urgent) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                        color = if (urgent) Color.White else MaterialTheme.colorScheme.tertiary
                    )
                    contact.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = subtle) }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (urgent) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = t(if (expanded) "Hide options" else "Show options"),
                        tint = if (urgent) Color.White else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onCall,
                            colors = if (urgent) {
                                ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PrimaryRed)
                            } else {
                                ButtonDefaults.buttonColors()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(t("Call"), modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(
                            onClick = onCopy,
                            colors = if (urgent) ButtonDefaults.outlinedButtonColors(contentColor = Color.White) else ButtonDefaults.outlinedButtonColors(),
                            border = if (urgent) BorderStroke(1.dp, Color.White) else ButtonDefaults.outlinedButtonBorder(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(t("Copy number"), modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    if (canEdit) {
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = onEdit) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(t("Edit"), modifier = Modifier.padding(start = 4.dp))
                            }
                            TextButton(onClick = onDelete) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(t("Delete"), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotlineEditDialog(
    existing: EmergencyContact?,
    categories: List<HotlineCategory>,
    isSaving: Boolean,
    onSave: (EmergencyContactInput) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(existing?.phone.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var categoryId by rememberSaveable { mutableStateOf(existing?.categoryId ?: categories.firstOrNull()?.id) }
    var order by rememberSaveable { mutableStateOf((existing?.sortOrder ?: 100).toString()) }
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == categoryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(if (existing == null) "Add hotline" else "Edit hotline")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.name?.let { t(it) } ?: t("Other"),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(t("Category")) },
                        leadingIcon = { Icon(HotlineIcon.fromKey(selected?.icon).vector(), contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(t(option.name)) },
                                leadingIcon = { Icon(HotlineIcon.fromKey(option.icon).vector(), contentDescription = null) },
                                onClick = { categoryId = option.id; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    label = { Text(t("Name")) },
                    placeholder = { Text(t("e.g. BFP Santa Margarita Fire Station")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(40) },
                    label = { Text(t("Phone number")) },
                    placeholder = { Text(t("e.g. 0917 123 4567")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text(t("Note (optional)")) },
                    placeholder = { Text(t("e.g. 24/7, near the municipal hall")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = order,
                    onValueChange = { v -> order = v.filter(Char::isDigit).take(4) },
                    label = { Text(t("Order in list (lower shows first)")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onSave(EmergencyContactInput(name, categoryId, phone, note, order.toIntOrNull() ?: 100)) }
            ) { Text(t(if (isSaving) "Saving…" else "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel")) } }
    )
}

/** Opens the dialer with the number filled in; the user still presses the call button there. */
private fun dial(context: Context, phone: String) {
    val number = phone.filter { it.isDigit() || it == '+' }
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$number".toUri()))
    } catch (_: ActivityNotFoundException) {
        // No dialer (e.g. some tablets); copying the number still works.
    }
}

private fun copyNumber(context: Context, phone: String, toast: String) {
    context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("Phone number", phone))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}
