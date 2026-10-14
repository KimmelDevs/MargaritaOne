package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.util.Dates
import java.util.Calendar
import java.util.TimeZone

/**
 * A button showing the chosen date and time (or [placeholder]); tapping it opens a calendar, then a clock.
 * [onChange] receives epoch millis in the phone's time zone, or null when cleared.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(
    millis: Long?,
    placeholder: String,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(0) } // 0 = closed, 1 = picking date, 2 = picking time
    var pickedDateUtc by remember { mutableStateOf<Long?>(null) }
    val initial = Calendar.getInstance().apply { millis?.let { timeInMillis = it } }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Filled.Event, contentDescription = null)
            Text(
                millis?.let { Dates.formatMillis(it) } ?: t(placeholder),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (millis != null) {
            IconButton(onClick = { onChange(null) }) {
                Icon(Icons.Filled.Clear, contentDescription = t("Clear"))
            }
        }
    }

    if (step == 1) {
        // The date picker works in UTC midnight; convert the local date to that.
        val utcToday = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val dateState = rememberDatePickerState(initialSelectedDateMillis = utcToday)
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateUtc = dateState.selectedDateMillis
                    step = if (pickedDateUtc != null) 2 else 0
                }) { Text(t("Next")) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(t("Cancel")) } }
        ) {
            DatePicker(state = dateState)
        }
    }

    if (step == 2) {
        val timeState = rememberTimePickerState(
            initialHour = initial.get(Calendar.HOUR_OF_DAY),
            initialMinute = initial.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { step = 0 },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = pickedDateUtc ?: return@TextButton
                    }
                    val local = Calendar.getInstance().apply {
                        clear()
                        set(
                            utcDate.get(Calendar.YEAR),
                            utcDate.get(Calendar.MONTH),
                            utcDate.get(Calendar.DAY_OF_MONTH),
                            timeState.hour,
                            timeState.minute
                        )
                    }
                    onChange(local.timeInMillis)
                    step = 0
                }) { Text(t("OK")) }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text(t("Cancel")) } }
        )
    }
}
