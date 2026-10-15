package com.kimmeldevs.margaritaone.ui.screens.report

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Map
import com.kimmeldevs.margaritaone.ui.components.GradientFab
import com.kimmeldevs.margaritaone.ui.components.RoundIconButton
import com.kimmeldevs.margaritaone.ui.components.ScreenHeader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.MessageBox
import com.kimmeldevs.margaritaone.ui.components.OutboxBanner
import androidx.compose.foundation.layout.Column
import com.kimmeldevs.margaritaone.ui.components.RefreshOnResume
import com.kimmeldevs.margaritaone.ui.components.SearchTopBar
import com.kimmeldevs.margaritaone.ui.components.SkeletonList
import com.kimmeldevs.margaritaone.ui.components.UiStateContent
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    currentUserId: String,
    onNewReportClick: () -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportListViewModel = viewModel(
        factory = ReportListViewModel.factory(allReports = false, currentUserId = currentUserId)
    )
) {
    val state by viewModel.reports.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.reports.isRefreshing.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val showMap by viewModel.showMap.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()

    RefreshOnResume { viewModel.reports.refreshIfLoaded() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                eyebrow = "Santa Margarita",
                title = "My reports",
                query = filters.query,
                onQueryChange = { q -> viewModel.updateFilters { it.copy(query = q) } },
                searchPlaceholder = "Search title, place, or reference no."
            ) {
                RoundIconButton(
                    if (showMap) Icons.AutoMirrored.Filled.List else Icons.Outlined.Map,
                    t(if (showMap) "Show as list" else "Show on map"),
                    onClick = viewModel::toggleMap
                )
                RoundIconButton(Icons.Filled.Tune, t("Filters"), badge = filters.activeCount()) { showFilters = true }
            }
        },
        floatingActionButton = {
            GradientFab(text = t("Report a problem"), icon = Icons.Filled.Add, onClick = onNewReportClick)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutboxBanner(
                pending = pending,
                onSendNow = viewModel::sendPendingNow,
                onDiscard = viewModel::discardPending,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            UiStateContent(
                state = state,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.reports.refresh() },
                loading = { SkeletonList() }
            ) { data ->
                if (data.reports.isEmpty()) {
                    MessageBox("You haven't reported anything yet.\nTap \"Report a problem\" to send one to the LGU.")
                } else {
                    ReportsBody(
                        showMap = showMap,
                        data = data,
                        filters = filters,
                        viewModel = viewModel,
                        onReportClick = onReportClick,
                        emptyText = "No reports match these filters."
                    ) { all ->
                        item(key = "statusPills") {
                            StatusFilterPills(all = all, selected = filters.status, onSelect = { status ->
                                viewModel.updateFilters { it.copy(status = status) }
                            })
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        ReportFilterSheet(
            filters = filters,
            onChange = viewModel::updateFilters,
            showStaffOptions = false,
            onDismiss = { showFilters = false },
            barangays = (state as? UiState.Success)?.data?.barangays.orEmpty()
        )
    }
}
