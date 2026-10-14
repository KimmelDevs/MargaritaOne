package com.kimmeldevs.margaritaone.ui.screens.hotlines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.data.model.EmergencyContact
import com.kimmeldevs.margaritaone.data.model.HotlineCategory
import com.kimmeldevs.margaritaone.data.model.HotlineCategoryInput
import com.kimmeldevs.margaritaone.data.model.HotlineIcon
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.ConfirmDialog

fun HotlineIcon.vector(): ImageVector = when (this) {
    HotlineIcon.WARNING -> Icons.Filled.Warning
    HotlineIcon.FIRE -> Icons.Filled.LocalFireDepartment
    HotlineIcon.POLICE -> Icons.Filled.LocalPolice
    HotlineIcon.MEDICAL -> Icons.Filled.LocalHospital
    HotlineIcon.AMBULANCE -> Icons.Filled.MedicalServices
    HotlineIcon.FLOOD -> Icons.Filled.Flood
    HotlineIcon.POWER -> Icons.Filled.Power
    HotlineIcon.WATER -> Icons.Filled.WaterDrop
    HotlineIcon.RESCUE -> Icons.Filled.Support
    HotlineIcon.GOVERNMENT -> Icons.Filled.AccountBalance
    HotlineIcon.SCHOOL -> Icons.Filled.School
    HotlineIcon.PHONE -> Icons.Filled.Phone
}

/** Admin-only: list, add, edit, and delete hotline categories. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotlineCategoriesSheet(
    categories: List<HotlineCategory>,
    contacts: List<EmergencyContact>,
    isSaving: Boolean,
    onSave: (existing: HotlineCategory?, input: HotlineCategoryInput, onDone: () -> Unit) -> Unit,
    onDelete: (HotlineCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var editing by remember { mutableStateOf<CategoryEditTarget?>(null) }
    var pendingDelete by remember { mutableStateOf<HotlineCategory?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(t("Hotline categories"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                t("Categories group the hotlines. Deleting one moves its hotlines to \"Other\"."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            categories.forEach { category ->
                val count = contacts.count { it.categoryId == category.id }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        HotlineIcon.fromKey(category.icon).vector(),
                        contentDescription = null,
                        tint = if (category.isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(t(category.name), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            t("%d hotlines", count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { editing = CategoryEditTarget(category) }) {
                        Icon(Icons.Filled.Edit, contentDescription = t("Edit"))
                    }
                    IconButton(onClick = { pendingDelete = category }) {
                        Icon(Icons.Filled.Delete, contentDescription = t("Delete"), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            OutlinedButton(
                onClick = { editing = CategoryEditTarget(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(t("Add category"), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }

    editing?.let { (category) ->
        CategoryEditDialog(
            existing = category,
            isSaving = isSaving,
            onSave = { input -> onSave(category, input) { editing = null } },
            onDismiss = { editing = null }
        )
    }
    pendingDelete?.let { category ->
        ConfirmDialog(
            title = "Delete category?",
            message = t("\"%s\" will be removed. Its hotlines will move to \"Other\".", t(category.name)),
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                onDelete(category)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

/** Wrapper so "add new" (null category) is distinguishable from "no dialog". */
private data class CategoryEditTarget(val category: HotlineCategory?)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditDialog(
    existing: HotlineCategory?,
    isSaving: Boolean,
    onSave: (HotlineCategoryInput) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(existing?.icon ?: HotlineIcon.PHONE.key) }
    var urgent by rememberSaveable { mutableStateOf(existing?.isUrgent ?: false) }
    var order by rememberSaveable { mutableStateOf((existing?.sortOrder ?: 100).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t(if (existing == null) "Add category" else "Edit category")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(t("Name")) },
                    placeholder = { Text(t("e.g. Barangay hotlines")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(t("Icon"), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HotlineIcon.entries.forEach { option ->
                        FilledIconToggleButton(
                            checked = icon == option.key,
                            onCheckedChange = { icon = option.key },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(option.vector(), contentDescription = option.key)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(t("Urgent"))
                        Text(
                            t("Shown in red, like the national emergency line"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = urgent, onCheckedChange = { urgent = it })
                }
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
                onClick = { onSave(HotlineCategoryInput(name, icon, urgent, order.toIntOrNull() ?: 100)) }
            ) { Text(t(if (isSaving) "Saving…" else "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("Cancel")) } }
    )
}
