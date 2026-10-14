package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.i18n.AppLanguage
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.LocalSetLanguage

/** English / Filipino / Winaray switch (#27). Takes effect immediately and is remembered on this phone. */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val current = LocalLanguage.current
    val setLanguage = LocalSetLanguage.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        AppLanguage.entries.forEach { language ->
            FilterChip(
                selected = current == language,
                onClick = { setLanguage(language) },
                label = { Text(language.nativeName) }
            )
        }
    }
}
