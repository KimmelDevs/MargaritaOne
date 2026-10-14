package com.kimmeldevs.margaritaone.ui.screens.report

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.data.model.ReportDto
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.TownMap
import com.kimmeldevs.margaritaone.ui.components.ReportCard
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportFilters
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListData
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListViewModel
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val SOURCE_ID = "reports"
private const val LAYER_ID = "reports-pins"

/** Pin colours by status (fixed so they read on both map styles). */
private val STATUS_COLORS = mapOf(
    ReportStatus.PENDING to Color(0xFFD32F2F),
    ReportStatus.IN_PROGRESS to Color(0xFFF9A825),
    ReportStatus.RESOLVED to Color(0xFF2E7D32),
    ReportStatus.REJECTED to Color(0xFF757575),
    ReportStatus.CANCELLED to Color(0xFF9E9E9E)
)

/** The reports on a map (#8): same data and filters as the list; tap a pin to see the report. */
@Composable
fun ReportMapContent(
    data: ReportListData,
    filters: ReportFilters,
    viewModel: ReportListViewModel,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val located = viewModel.apply(data.reports, filters).filter { it.latitude != null && it.longitude != null }
    // The click listener is registered once; this keeps its lookup pointing at the latest data.
    val byId = rememberUpdatedState(located.associateBy { it.id })
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var selected by remember { mutableStateOf<ReportDto?>(null) }
    var fitted by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        TownMap(
            modifier = Modifier.fillMaxSize(),
            onReady = { m, style ->
                style.addSource(GeoJsonSource(SOURCE_ID))
                style.addLayer(
                    CircleLayer(LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.circleRadius(8f),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                        PropertyFactory.circleColor(
                            Expression.match(
                                Expression.get("status"),
                                Expression.color(STATUS_COLORS.getValue(ReportStatus.REJECTED).toArgb()),
                                *STATUS_COLORS.map { (status, color) ->
                                    Expression.stop(status.name, Expression.color(color.toArgb()))
                                }.toTypedArray()
                            )
                        )
                    )
                )
                m.addOnMapClickListener { point ->
                    val hit = m.queryRenderedFeatures(m.projection.toScreenLocation(point), LAYER_ID).firstOrNull()
                    selected = hit?.getStringProperty("id")?.let { id -> byId.value[id] }
                    hit != null
                }
                map = m
            }
        )

        LaunchedEffect(map, located) {
            val m = map ?: return@LaunchedEffect
            val features = located.map { r ->
                Feature.fromGeometry(Point.fromLngLat(r.longitude!!, r.latitude!!)).apply {
                    addStringProperty("id", r.id)
                    addStringProperty("status", r.status.name)
                }
            }
            m.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)?.setGeoJson(FeatureCollection.fromFeatures(features))
            if (!fitted && located.isNotEmpty()) {
                fitted = true
                val points = located.map { LatLng(it.latitude!!, it.longitude!!) }
                if (points.size == 1) {
                    m.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 15.0))
                } else {
                    m.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), 120))
                }
            }
        }

        Legend(modifier = Modifier.align(Alignment.TopStart).padding(12.dp))

        if (located.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            ) {
                Text(
                    t("No reports with a location match these filters."),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        selected?.let { report ->
            ReportCard(
                report = report,
                onClick = { onReportClick(report.id) },
                thumbnailUrl = report.photoPath?.let { data.thumbnails[it] },
                isUpdated = viewModel.isUpdated(report),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
            )
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 3.dp, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            listOf(ReportStatus.PENDING, ReportStatus.IN_PROGRESS, ReportStatus.RESOLVED).forEach { status ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(STATUS_COLORS.getValue(status), CircleShape)
                    )
                    Text(t(status.label), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
