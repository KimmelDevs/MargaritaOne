package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.theme.LocalSetThemeMode
import com.kimmeldevs.margaritaone.ui.theme.LocalThemeMode
import com.kimmeldevs.margaritaone.util.ThemeMode

/** System / Light / Dark switch. Takes effect immediately and is remembered on this phone. */
@Composable
fun ThemeModePicker(modifier: Modifier = Modifier) {
    val current = LocalThemeMode.current
    val setMode = LocalSetThemeMode.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = current == mode,
                onClick = { setMode(mode) },
                leadingIcon = {
                    Icon(
                        when (mode) {
                            ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Filled.LightMode
                            ThemeMode.DARK -> Icons.Filled.DarkMode
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                label = { Text(t(mode.label)) }
            )
        }
    }
}
