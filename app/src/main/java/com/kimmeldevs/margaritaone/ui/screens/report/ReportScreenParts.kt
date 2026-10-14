package com.kimmeldevs.margaritaone.ui.screens.report

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
