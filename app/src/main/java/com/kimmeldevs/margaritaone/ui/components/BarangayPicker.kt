package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.i18n.t

/**
 * Dropdown of the admin-managed barangay list, so names are spelled the same everywhere (#17).
 * Until an admin adds barangays, it falls back to a plain text field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarangayPicker(
    barangays: List<Barangay>,
    selectedName: String,
    onSelect: (Barangay?) -> Unit,
    onFreeText: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    if (barangays.isEmpty()) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = onFreeText,
            label = { Text(t(label)) },
            singleLine = true,
            modifier = modifier.fillMaxWidth()
        )
        return
    }
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(t(label)) },
            placeholder = { Text(t("Choose your barangay")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(t("None")) }, onClick = { onSelect(null); expanded = false })
            barangays.forEach { barangay ->
                DropdownMenuItem(
                    text = { Text(barangay.name) },
                    onClick = { onSelect(barangay); expanded = false }
                )
            }
        }
    }
}
