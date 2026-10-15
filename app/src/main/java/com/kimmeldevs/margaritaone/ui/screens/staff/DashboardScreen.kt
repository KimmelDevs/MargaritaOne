package com.kimmeldevs.margaritaone.ui.screens.staff

import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Map
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.kimmeldevs.margaritaone.data.model.ReportDto
import com.kimmeldevs.margaritaone.ui.components.RoundIconButton
import com.kimmeldevs.margaritaone.ui.components.ScreenHeader
import com.kimmeldevs.margaritaone.ui.screens.report.StatusFilterPills
import com.kimmeldevs.margaritaone.ui.screens.report.activeCount
import com.kimmeldevs.margaritaone.ui.theme.DeepNavy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.data.model.ReportStatus
import com.kimmeldevs.margaritaone.i18n.t
import com.kimmeldevs.margaritaone.ui.components.RefreshOnResume
import com.kimmeldevs.margaritaone.ui.components.SearchTopBar
import com.kimmeldevs.margaritaone.ui.components.SkeletonList
import com.kimmeldevs.margaritaone.ui.components.UiStateContent
import com.kimmeldevs.margaritaone.ui.screens.report.FilterButton
import com.kimmeldevs.margaritaone.ui.screens.report.ReportFilterSheet
import com.kimmeldevs.margaritaone.ui.screens.report.MapToggleButton
import com.kimmeldevs.margaritaone.ui.screens.report.ReportsBody
import com.kimmeldevs.margaritaone.ui.viewmodel.ReportListViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.UiState
import com.kimmeldevs.margaritaone.i18n.I18n
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.util.Dates
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect

/**
 * Opens with just three stat tiles and the list. The tiles double as the status filter;
 * search is behind the top-bar icon and everything else is in the Filters sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentUserId: String,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportListViewModel = viewModel(
        factory = ReportListViewModel.factory(allReports = true, currentUserId = currentUserId)
    )
) {
    val state by viewModel.reports.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.reports.isRefreshing.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val showMap by viewModel.showMap.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val language = LocalLanguage.current
    val loaded = (state as? UiState.Success)?.data

    // Export CSV (#16): the user picks where to save; no storage permission needed.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && loaded != null) viewModel.exportCsv(uri, loaded, filters)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(I18n.tr(language, it))
            viewModel.clearMessage()
        }
    }

    RefreshOnResume { viewModel.reports.refreshIfLoaded() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ScreenHeader(
                eyebrow = "LGU dashboard",
                title = "Reports",
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
                Box {
                    RoundIconButton(Icons.Filled.MoreVert, t("More options")) { menuOpen = true }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(t("Export to CSV (Excel)")) },
                            leadingIcon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                            enabled = loaded != null,
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch("margaritaone-reports-${Dates.formatMillis(System.currentTimeMillis(), "yyyy-MM-dd")}.csv")
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        UiStateContent(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.reports.refresh() },
            modifier = Modifier.padding(innerPadding),
            loading = { SkeletonList() }
        ) { data ->
            ReportsBody(
                showMap = showMap,
                data = data,
                filters = filters,
                viewModel = viewModel,
                onReportClick = onReportClick,
                emptyText = "No reports here."
            ) { all ->
                item(key = "attention") { NeedsAttentionCard(all) }
                item(key = "statusPills") {
                    StatusFilterPills(all = all, selected = filters.status, onSelect = { status ->
                        viewModel.updateFilters { it.copy(status = status) }
                    })
                }
            }
        }
    }

    if (showFilters) {
        ReportFilterSheet(
            filters = filters,
            onChange = viewModel::updateFilters,
            showStaffOptions = true,
            onDismiss = { showFilters = false },
            barangays = loaded?.barangays.orEmpty()
        )
    }
}

/** Navy card: how many reports still need work, with a pending / in progress / resolved bar. */
@Composable
private fun NeedsAttentionCard(all: List<ReportDto>) {
    val pending = all.count { it.status == ReportStatus.PENDING }
    val inProgress = all.count { it.status == ReportStatus.IN_PROGRESS }
    val resolved = all.count { it.status == ReportStatus.RESOLVED }
    val total = (pending + inProgress + resolved).coerceAtLeast(1)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF0B4A93))))
            .padding(18.dp)
    ) {
        Text(t("Needs attention"), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text((pending + inProgress).toString(), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
            Text(
                t("active reports"),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(pending to SoftRed, inProgress to SoftBlue, resolved to Color.White.copy(alpha = 0.35f)).forEach { (count, color) ->
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .weight(count.toFloat() / total)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
            if (pending + inProgress + resolved == 0) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            LegendDot(SoftRed, t("%d Pending", pending))
            LegendDot(SoftBlue, t("%d In progress", inProgress))
            LegendDot(Color.White.copy(alpha = 0.5f), t("%d Resolved", resolved))
        }
    }
}

private val SoftRed = Color(0xFFFF5C74)
private val SoftBlue = Color(0xFF7DB4F5)

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
