package com.kimmeldevs.margaritaone.ui.screens.staff

import com.kimmeldevs.margaritaone.i18n.I18n
import com.kimmeldevs.margaritaone.i18n.LocalLanguage
import com.kimmeldevs.margaritaone.i18n.t
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kimmeldevs.margaritaone.data.model.Profile
import com.kimmeldevs.margaritaone.data.model.UserRole
import com.kimmeldevs.margaritaone.ui.components.ChoiceChipRow
import com.kimmeldevs.margaritaone.ui.components.EmptyListText
import com.kimmeldevs.margaritaone.ui.components.SkeletonList
import com.kimmeldevs.margaritaone.ui.components.RefreshOnResume
import com.kimmeldevs.margaritaone.ui.components.UiStateContent
import com.kimmeldevs.margaritaone.ui.viewmodel.UsersViewModel
import com.kimmeldevs.margaritaone.ui.viewmodel.matching

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    currentUserId: String,
    modifier: Modifier = Modifier,
    viewModel: UsersViewModel = viewModel(factory = UsersViewModel.Factory)
) {
    val state by viewModel.users.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.users.isRefreshing.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val roleFilter by viewModel.roleFilter.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val language = LocalLanguage.current
    var pendingChange by remember { mutableStateOf<Pair<Profile, UserRole>?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(I18n.tr(language, it))
            viewModel.clearMessage()
        }
    }

    RefreshOnResume { viewModel.users.refreshIfLoaded() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(t("Users")) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        UiStateContent(
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.users.refresh() },
            modifier = Modifier.padding(innerPadding),
            loading = { SkeletonList() }
        ) { users ->
            val visible = users.matching(query, roleFilter)
            val roleNames = UserRole.entries.associateWith { t(it.label) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(t("Search name, email, barangay")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    ChoiceChipRow(
                        options = UserRole.entries,
                        selected = roleFilter,
                        label = { role -> "${roleNames[role]} (${users.count { it.role == role }})" },
                        onSelect = viewModel::setRoleFilter,
                        allLabel = t("Everyone (%d)", users.size)
                    )
                }
                if (visible.isEmpty()) {
                    item { EmptyListText(t("No users found.")) }
                }
                items(visible, key = { it.id }) { user ->
                    UserRow(
                        user = user,
                        isSelf = user.id == currentUserId,
                        onRoleSelected = { role -> pendingChange = user to role }
                    )
                }
            }
        }
    }

    pendingChange?.let { (user, role) ->
        AlertDialog(
            onDismissRequest = { pendingChange = null },
            title = { Text(t("Change role?")) },
            text = {
                Text(t("Make %1\$s %2\$s?", user.displayName, t(role.label).lowercase()) + "\n\n" + t(roleHint(role)))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setRole(user, role)
                    pendingChange = null
                }) { Text(t("Confirm")) }
            },
            dismissButton = { TextButton(onClick = { pendingChange = null }) { Text(t("Cancel")) } }
        )
    }
}

private fun roleHint(role: UserRole): String = when (role) {
    UserRole.CITIZEN -> "They will lose access to all reports and staff tools."
    UserRole.STAFF -> "They will see every report, reporters' contact details, and can update report status."
    UserRole.ADMIN -> "They will also be able to post to the bulletin and change other users' roles."
}

@Composable
private fun UserRow(
    user: Profile,
    isSelf: Boolean,
    onRoleSelected: (UserRole) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSelf) t("%s (you)", user.displayName) else user.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                listOfNotNull(user.email, user.barangay).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            Box {
                TextButton(onClick = { menuOpen = true }, enabled = !isSelf) {
                    Text(t(user.role.label))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    UserRole.entries.filter { it != user.role }.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(t("Make %s", t(role.label).lowercase())) },
                            onClick = {
                                menuOpen = false
                                onRoleSelected(role)
                            }
                        )
                    }
                }
            }
        }
    }
}
