package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.data.PendingReport
import com.kimmeldevs.margaritaone.i18n.t

/** Reports saved without signal (#10): sent automatically when online; "Send now" retries immediately. */
@Composable
fun OutboxBanner(
    pending: List<PendingReport>,
    onSendNow: () -> Unit,
    onDiscard: (PendingReport) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pending.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    t(if (pending.size == 1) "1 report waiting to send" else "%d reports waiting to send", pending.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                TextButton(onClick = onSendNow) { Text(t("Send now")) }
            }
            Text(
                t("They'll be sent automatically when you're back online."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            pending.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("• ${item.report.title}", style = MaterialTheme.typography.bodyMedium)
                        item.lastError?.let {
                            Text(
                                t("Not sent: %s", t(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (item.lastError != null) {
                        TextButton(onClick = { onDiscard(item) }) {
                            Text(t("Discard"), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
