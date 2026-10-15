package com.kimmeldevs.margaritaone.ui.screens.report

import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportFilters
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListData
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListViewModel

/** Top-bar button that switches between the list and the map (#8). */
@Composable
fun MapToggleButton(showMap: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        if (showMap) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = t("Show as list"))
        } else {
            Icon(Icons.Filled.Map, contentDescription = t("Show on map"))
        }
    }
}

/** The list or the map, sharing the same data and filters. */
@Composable
fun ReportsBody(
    showMap: Boolean,
    data: ReportListData,
    filters: ReportFilters,
    viewModel: ReportListViewModel,
    onReportClick: (String) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    header: androidx.compose.foundation.lazy.LazyListScope.(all: List<com.kimmeldevs.margaritaone.data.model.ReportDto>) -> Unit = {}
) {
    if (showMap) {
        ReportMapContent(data = data, filters = filters, viewModel = viewModel, onReportClick = onReportClick, modifier = modifier)
    } else {
        ReportListContent(
            data = data,
            filters = filters,
            viewModel = viewModel,
            onReportClick = onReportClick,
            emptyText = emptyText,
            modifier = modifier,
            header = header
        )
    }
}

/** "All 4 · Pending 2 · In progress 2 · Resolved 0" pills that set the status filter. */
@Composable
fun StatusFilterPills(
    all: List<com.kimmeldevs.margaritaone.data.model.ReportDto>,
    selected: com.kimmeldevs.margaritaone.data.model.ReportStatus?,
    onSelect: (com.kimmeldevs.margaritaone.data.model.ReportStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    val counts = all.groupingBy { it.status }.eachCount()
    val statuses = listOf(
        com.kimmeldevs.margaritaone.data.model.ReportStatus.PENDING,
        com.kimmeldevs.margaritaone.data.model.ReportStatus.IN_PROGRESS,
        com.kimmeldevs.margaritaone.data.model.ReportStatus.RESOLVED
    ) + com.kimmeldevs.margaritaone.data.model.ReportStatus.entries.filter {
        it !in listOf(
            com.kimmeldevs.margaritaone.data.model.ReportStatus.PENDING,
            com.kimmeldevs.margaritaone.data.model.ReportStatus.IN_PROGRESS,
            com.kimmeldevs.margaritaone.data.model.ReportStatus.RESOLVED
        ) && (counts[it] ?: 0) > 0
    }
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        item {
            com.kimmeldevs.margaritaone.ui.components.FilterPill(
                label = "${t("All")} ${all.size}",
                selected = selected == null,
                onClick = { onSelect(null) }
            )
        }
        items(statuses.size) { index ->
            val status = statuses[index]
            com.kimmeldevs.margaritaone.ui.components.FilterPill(
                label = "${t(status.label)} ${counts[status] ?: 0}",
                selected = selected == status,
                onClick = { onSelect(if (selected == status) null else status) }
            )
        }
    }
}

