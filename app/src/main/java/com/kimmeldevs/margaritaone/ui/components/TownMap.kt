package com.kimmeldevs.margaritaone.ui.components

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kimmeldevs.margaritaone.ui.theme.LocalThemeMode
import com.kimmeldevs.margaritaone.util.ThemeMode
import androidx.compose.foundation.isSystemInDarkTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Poblacion, Santa Margarita, Samar — where the map opens when there's nothing else to show. */
val TOWN_CENTER = LatLng(12.0370, 124.6590)

/**
 * Free OpenFreeMap vector tiles (no API key, no billing) — the same MapLibre engine mapcn uses on the web.
 * Light/dark style follows the app's theme setting.
 */
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

/**
 * A MapLibre map inside Compose. [onReady] runs once the style has loaded, with the map and its style,
 * so callers can add sources/layers and listeners.
 */
@Composable
fun TownMap(
    modifier: Modifier = Modifier,
    center: LatLng = TOWN_CENTER,
    zoom: Double = 13.0,
    onReady: (MapLibreMap, Style) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dark = when (LocalThemeMode.current) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(Bundle()) }
    }

    // Forward the screen's lifecycle to the map (it manages GL resources and tile downloads).
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    DisposableEffect(dark) {
        mapView.getMapAsync { map ->
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false
            if (map.style == null) {
                map.cameraPosition = CameraPosition.Builder().target(center).zoom(zoom).build()
            }
            map.setStyle(Style.Builder().fromUri(if (dark) STYLE_DARK else STYLE_LIGHT)) { style ->
                onReady(map, style)
            }
        }
        onDispose { }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
