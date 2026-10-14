package com.kimmeldevs.margaritaone.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.i18n.I18n
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.ChoiceChipRow
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog
import com.kimmeldevs.margaritaone.ui.components.EmptyListText
import com.kimmeldevs.margaritaone.ui.components.ListSectionHeader
import com.kimmeldevs.margaritaone.ui.components.SkeletonList
import com.kimmeldevs.margaritaone.ui.components.UiStateContent
import com.kimmeldevs.margaritaone.ui.viewmodel.ActivityLogViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.AuditFilter
import com.kimmeldevs.margaritaone.ui.viewmodel.BarangaysViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.label
import com.kimmeldevs.margaritaone.util.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text(t(title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
            }
        }
    )
}

/** Admin: add, rename, and delete barangays (#17). */
@Composable
fun BarangaysScreen(
    onBackClick: () -> Unit,
    viewModel: BarangaysViewModel = viewModel(factory = BarangaysViewModel.Factory)
) {
    val state by viewModel.barangays.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.barangays.isRefreshing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val language = LocalLanguage.current
    var editing by remember { mutableStateOf<BarangayEditTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<Barangay?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(I18n.tr(language, it))
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = { BackTopBar("Barangays", onBackClick) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = BarangayEditTarget(null) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(t("Add barangay")) }
            )
        }
    ) { innerPadding ->
        UiStateContent(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.barangays.refresh() },
            modifier = Modifier.padding(innerPadding),
            loading = { SkeletonList() }
        ) { barangays ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        t("Residents pick from this list on their profile and when reporting, so names stay consistent for filters and exports."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (barangays.isEmpty()) {
                    item { EmptyListText("No barangays yet. Tap \"Add barangay\".") }
                }
                items(barangays, key = { it.id }) { barangay ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            Text(barangay.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editing = BarangayEditTarget(barangay) }) {
                                Icon(Icons.Filled.Edit, contentDescription = t("Edit"))
                            }
                            IconButton(onClick = { pendingDelete = barangay }) {
                                Icon(Icons.Filled.Delete, contentDescription = t("Delete"), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { (barangay) ->
        var name by rememberSaveable { mutableStateOf(barangay?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(t(if (barangay == null) "Add barangay" else "Rename barangay")) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text(t("Name")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.save(barangay, name) { editing = null } }) { Text(t("Save")) } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(t("Cancel")) } }
        )
    }
    pendingDelete?.let { barangay ->
        ConfirmDialog(
            title = "Delete barangay?",
            message = t("\"%s\" will be removed from the list. Existing reports keep working but lose this barangay.", barangay.name),
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.delete(barangay)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

private data class BarangayEditTarget(val barangay: Barangay?)

/** Admin: who changed what and when (#19). Read-only; rows come from database triggers. */
@Composable
fun ActivityLogScreen(
    onBackClick: () -> Unit,
    viewModel: ActivityLogViewModel = viewModel(factory = ActivityLogViewModel.Factory)
) {
    val state by viewModel.entries.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.entries.isRefreshing.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    Scaffold(topBar = { BackTopBar("Activity log", onBackClick) }) { innerPadding ->
        UiStateContent(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.entries.refresh() },
            modifier = Modifier.padding(innerPadding),
            loading = { SkeletonList() }
        ) { all ->
            val visible = viewModel.visible(all, filter)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    ChoiceChipRow(
                        options = AuditFilter.entries,
                        selected = filter,
                        label = { it.label },
                        onSelect = viewModel::setFilter
                    )
                }
                if (visible.isEmpty()) {
                    item { EmptyListText("Nothing logged yet.") }
                }
                visible.groupBy { Dates.bucket(it.createdAt) }.forEach { (bucket, entries) ->
                    item(key = "h_${bucket.name}") { ListSectionHeader(bucket.label()) }
                    items(entries, key = { it.id }) { entry ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            // Summaries are written in English by the database; they are names and titles, not UI text.
                            Text(entry.summary, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${entry.actorName ?: t("System")} · ${Dates.dateTime(entry.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
