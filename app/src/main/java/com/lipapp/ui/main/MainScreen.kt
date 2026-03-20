package com.lipapp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showAddNetwork by remember { mutableStateOf(false) }
    var showJoinChannel by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }

    state.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Sidebar(
                items = state.sidebar,
                currentTarget = state.currentTarget,
                unread = state.unread,
                onSelectChannel = { net, ch ->
                    viewModel.selectTarget(com.lipapp.data.model.ChatTarget.Channel(net, ch))
                    scope.launch { drawerState.close() }
                },
                onSelectQuery = { net, nick ->
                    viewModel.selectTarget(com.lipapp.data.model.ChatTarget.Query(net, nick))
                    scope.launch { drawerState.close() }
                },
                onConnectNetwork = viewModel::connectNetwork,
                onDisconnectNetwork = viewModel::disconnectNetwork,
                onDeleteNetwork = { name ->
                    confirmAction = ConfirmAction("Delete network '$name'?") {
                        viewModel.deleteNetwork(name)
                    }
                },
                onLeaveChannel = { net, ch ->
                    confirmAction = ConfirmAction("Leave $ch?") {
                        viewModel.leaveChannel(net, ch)
                    }
                },
                onCloseQuery = { net, nick ->
                    confirmAction = ConfirmAction("Close conversation with $nick?") {
                        viewModel.closeQuery(net, nick)
                    }
                },
                onAddNetwork = { showAddNetwork = true },
                onJoinChannel = { showJoinChannel = true },
            )
        },
    ) {
        val titlePrefix = if (state.unread.isNotEmpty()) "* " else ""
        val title = state.currentTarget?.let { target ->
            "$titlePrefix${target.network} / ${target.displayName}"
        } ?: "${titlePrefix}LipApp"

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (state.currentTarget != null) {
                            IconButton(onClick = { showSearch = !showSearch }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                if (state.darkMode) Icons.Default.LightMode
                                else Icons.Default.DarkMode,
                                contentDescription = "Toggle dark mode",
                            )
                        }
                    },
                )
            },
            snackbarHost = {
                state.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        },
                    ) { Text(error) }
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
            ) {
                if (showSearch && state.currentTarget != null) {
                    SearchBar(
                        query = state.searchQuery,
                        isSearching = state.isSearching,
                        onSearch = viewModel::search,
                        onClose = {
                            showSearch = false
                            viewModel.clearSearch()
                        },
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (state.currentTarget != null) {
                        MessageList(
                            messages = state.searchResults ?: state.messages,
                            hasMore = state.hasMoreMessages && state.searchResults == null,
                            isLoadingMore = state.isLoadingMore,
                            isLoading = state.isLoadingMessages,
                            myNick = state.myNicks[state.currentTarget?.network],
                            darkMode = state.darkMode,
                            onLoadMore = viewModel::loadMoreMessages,
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            Text(
                                "Select a channel or conversation",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (state.currentTarget != null) {
                    MessageInput(onSend = viewModel::sendMessage)
                }
            }
        }
    }

    if (showAddNetwork) {
        AddNetworkDialog(
            onDismiss = { showAddNetwork = false },
            onAdd = { name, host, port, nick, password, tls ->
                viewModel.addNetwork(name, host, port, nick, password, tls)
                showAddNetwork = false
            },
        )
    }

    if (showJoinChannel) {
        JoinChannelDialog(
            networks = state.sidebar.map { it.network.name },
            currentNetwork = state.currentTarget?.network,
            onDismiss = { showJoinChannel = false },
            onJoin = { network, channel ->
                viewModel.joinChannel(network, channel)
                showJoinChannel = false
            },
        )
    }

    confirmAction?.let { action ->
        ConfirmDialog(
            message = action.message,
            onConfirm = {
                action.onConfirm()
                confirmAction = null
            },
            onDismiss = { confirmAction = null },
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onClose: () -> Unit,
) {
    var text by remember { mutableStateOf(query) }

    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search messages...") },
                singleLine = true,
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch(text) },
                ),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close search")
            }
        }
    }
}

data class ConfirmAction(val message: String, val onConfirm: () -> Unit)
