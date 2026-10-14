package com.kimmeldevs.margaritaone.ui.screens.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kimmeldevs.margaritaone.data.model.Barangay
import com.kimmeldevs.margaritaone.data.model.ReportCategory
import com.kimmeldevs.margaritaone.data.model.ReportDto
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.ChoiceChipRow
import com.kimmeldevs.margaritaone.ui.components.EmptyListText
import com.kimmeldevs.margaritaone.ui.components.ListSectionHeader
import com.kimmeldevs.margaritaone.ui.components.ReportCard
import com.kimmeldevs.margaritaone.ui.viewmodel.DateRange
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportFilters
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListData
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportSort
import com.kimmeldevs.margaritaone.ui.viewmodel.label
import com.kimmeldevs.margaritaone.util.Dates

/**
 * How many sheet filters differ from the defaults (badge on the Filters icon).
 * Search lives in the top bar and status in the stat tiles / count chips, so neither is counted here.
 */
fun ReportFilters.activeCount(): Int = listOf(
    dateRange != DateRange.ALL,
    category != null,
    assignedToMe,
    sort != ReportSort.NEWEST,
    barangayId != null
).count { it }

/** Top-bar Filters icon with a count badge. */
@Composable
fun FilterButton(filters: ReportFilters, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        val count = filters.activeCount()
        BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
            Icon(Icons.Filled.FilterList, contentDescription = t("Filters"))
        }
    }
}

/**
 * The reports list: optional [header] (stat tiles / status counts), a single row of the filters currently on
 * (each removable), then the reports grouped under Today / Yesterday / ... headers.
 * Everything else lives in [ReportFilterSheet] so the screen opens uncluttered.
 */
@Composable
fun ReportListContent(
    data: ReportListData,
    filters: ReportFilters,
    viewModel: ReportListViewModel,
    onReportClick: (String) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    header: LazyListScope.(all: List<ReportDto>) -> Unit = {}
) {
    val visible = viewModel.apply(data.reports, filters)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp)
    ) {
        header(data.reports)

        if (filters.activeCount() > 0) {
            item(key = "activeFilters") {
                ActiveFilterChips(filters = filters, barangayNames = data.barangayNames, onChange = viewModel::updateFilters)
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") { EmptyListText(emptyText) }
        }

        // Grouped by day (#8)
        visible
            .groupBy { Dates.bucket(viewModel.groupKey(it, filters.sort)) }
            .forEach { (bucket, reports) ->
                item(key = "header_${bucket.name}") { ListSectionHeader(bucket.label()) }
                items(reports, key = { it.id }) { report ->
                    ReportCard(
                        report = report,
                        onClick = { onReportClick(report.id) },
                        thumbnailUrl = report.photoPath?.let { data.thumbnails[it] },
                        isUpdated = viewModel.isUpdated(report)
                    )
                }
            }
    }
}

/** One removable chip per active filter, plus "Clear all". */
@Composable
private fun ActiveFilterChips(
    filters: ReportFilters,
    barangayNames: Map<String, String>,
    onChange: ((ReportFilters) -> ReportFilters) -> Unit
) {
    data class Active(val label: String, val clear: (ReportFilters) -> ReportFilters)

    val active = buildList {
        if (filters.dateRange != DateRange.ALL) add(Active(t(filters.dateRange.label)) { f -> f.copy(dateRange = DateRange.ALL) })
        filters.category?.let { add(Active(t(it.label)) { f -> f.copy(category = null) }) }
        filters.barangayId?.let { id -> add(Active(barangayNames[id] ?: t("Barangay")) { f -> f.copy(barangayId = null) }) }
        if (filters.assignedToMe) add(Active(t("Assigned to me")) { f -> f.copy(assignedToMe = false) })
        if (filters.sort != ReportSort.NEWEST) add(Active(t(filters.sort.label)) { f -> f.copy(sort = ReportSort.NEWEST) })
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        items(active) { chip ->
            InputChip(
                selected = true,
                onClick = { onChange(chip.clear) },
                label = { Text(chip.label) },
                trailingIcon = { Icon(Icons.Filled.Close, contentDescription = t("Remove filter"), modifier = Modifier.size(16.dp)) }
            )
        }
        if (active.size > 1) {
            item {
                TextButton(onClick = { onChange { f -> ReportFilters(query = f.query, status = f.status) } }) { Text(t("Clear all")) }
            }
        }
    }
}

/** Bottom sheet with every filter and the sort order. [showStaffOptions] adds "Assigned to me". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFilterSheet(
    filters: ReportFilters,
    onChange: ((ReportFilters) -> ReportFilters) -> Unit,
    showStaffOptions: Boolean,
    onDismiss: () -> Unit,
    barangays: List<Barangay> = emptyList()
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(t("Filters"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            SheetSection("Sort") {
                ChoiceChipRow(
                    options = ReportSort.entries,
                    selected = filters.sort,
                    label = { it.label },
                    onSelect = { s -> onChange { it.copy(sort = s ?: ReportSort.NEWEST) } },
                    allLabel = null
                )
            }
            SheetSection("Date") {
                ChoiceChipRow(
                    options = DateRange.entries,
                    selected = filters.dateRange,
                    label = { it.label },
                    onSelect = { d -> onChange { it.copy(dateRange = d ?: DateRange.ALL) } },
                    allLabel = null
                )
            }
            SheetSection("Category") {
                ChoiceChipRow(
                    options = ReportCategory.entries,
                    selected = filters.category,
                    label = { it.label },
                    onSelect = { c -> onChange { it.copy(category = c) } },
                    allLabel = "All categories"
                )
            }
            if (barangays.isNotEmpty()) {
                SheetSection("Barangay") {
                    ChoiceChipRow(
                        options = barangays,
                        selected = barangays.firstOrNull { it.id == filters.barangayId },
                        label = { it.name },
                        onSelect = { b -> onChange { it.copy(barangayId = b?.id) } },
                        allLabel = "All barangays"
                    )
                }
            }
            if (showStaffOptions) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(t("Only reports assigned to me"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = filters.assignedToMe,
                        onCheckedChange = { on -> onChange { it.copy(assignedToMe = on) } }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = { onChange { f -> ReportFilters(query = f.query, status = f.status) } },
                    modifier = Modifier.weight(1f)
                ) { Text(t("Reset")) }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(t("Done")) }
            }
        }
    }
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(t(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/** Small tappable status counter, used as the status filter on My reports. */
@Composable
fun StatusCountChip(count: Int, status: ReportStatus, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$count ${t(status.label)}") }
    )
}
