package com.kimmeldevs.margaritaone.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.util.LatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * Full-screen map with a pin fixed in the middle: drag the map until the pin sits on the problem (#9).
 * Useful when GPS is off or inaccurate, or when reporting something you saw elsewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    initial: LatLng?,
    onPicked: (LatLng) -> Unit,
    onDismiss: () -> Unit
) {
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t("Choose the location")) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back"))
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TownMap(
                    modifier = Modifier.fillMaxSize(),
                    center = initial?.let { org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) } ?: TOWN_CENTER,
                    zoom = if (initial != null) 17.0 else 14.0,
                    onReady = { m, _ -> map = m }
                )
                // The pin's tip marks the chosen point, so lift it by half its height.
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-20).dp)
                        .size(40.dp)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            t("Move the map so the pin is on the problem."),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Button(
                        onClick = {
                            map?.cameraPosition?.target?.let { onPicked(LatLng(it.latitude, it.longitude)) }
                        },
                        enabled = map != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(t("Use this location"))
                    }
                }
            }
        }
    }
}
