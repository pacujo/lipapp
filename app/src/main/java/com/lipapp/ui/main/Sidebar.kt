package com.lipapp.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.lipapp.data.model.ChatTarget
import com.lipapp.data.model.SidebarItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Sidebar(
    items: List<SidebarItem>,
    currentTarget: ChatTarget?,
    unread: Set<String>,
    onSelectChannel: (network: String, channel: String) -> Unit,
    onSelectQuery: (network: String, nick: String) -> Unit,
    onConnectNetwork: (String) -> Unit,
    onDisconnectNetwork: (String) -> Unit,
    onDeleteNetwork: (String) -> Unit,
    onLeaveChannel: (network: String, channel: String) -> Unit,
    onCloseQuery: (network: String, nick: String) -> Unit,
    onAddNetwork: () -> Unit,
    onJoinChannel: () -> Unit,
    onStartQuery: () -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Networks",
                style = MaterialTheme.typography.titleMedium,
            )
            Row {
                IconButton(onClick = onAddNetwork, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add network", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onJoinChannel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Tag, contentDescription = "Join channel", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onStartQuery, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Person, contentDescription = "Private message", modifier = Modifier.size(20.dp))
                }
            }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val net = item.network

                item {
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        ListItem(
                            headlineContent = {
                                val stateLabel = when (net.state) {
                                    "connected" -> ""
                                    "connecting" -> " (connecting)"
                                    else -> " (disconnected)"
                                }
                                Text(
                                    "${net.name}$stateLabel",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = if (net.state == "connected")
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { showMenu = true },
                            ),
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(48.dp, 0.dp),
                        ) {
                            if (net.state == "connected") {
                                DropdownMenuItem(
                                    text = { Text("Disconnect") },
                                    onClick = { showMenu = false; onDisconnectNetwork(net.name) },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Connect") },
                                    onClick = { showMenu = false; onConnectNetwork(net.name) },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { showMenu = false; onDeleteNetwork(net.name) },
                            )
                        }
                    }
                }

                items(item.channels) { channel ->
                    val key = "${net.name}/${channel.name}"
                    val isSelected = currentTarget == ChatTarget.Channel(net.name, channel.name)
                    val isUnread = key in unread
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    channel.name,
                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                )
                            },
                            selected = isSelected,
                            onClick = { onSelectChannel(net.name, channel.name) },
                            modifier = Modifier
                                .padding(start = 24.dp, end = 8.dp)
                                .combinedClickable(
                                    onClick = { onSelectChannel(net.name, channel.name) },
                                    onLongClick = { showMenu = true },
                                ),
                            icon = {
                                Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(72.dp, 0.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Leave") },
                                onClick = { showMenu = false; onLeaveChannel(net.name, channel.name) },
                            )
                        }
                    }
                }

                items(item.queries) { query ->
                    val key = "${net.name}/${query.nick}"
                    val isSelected = currentTarget == ChatTarget.Query(net.name, query.nick)
                    val isUnread = key in unread
                    var showMenu by remember { mutableStateOf(false) }

                    Box {
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    query.nick,
                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 1,
                                )
                            },
                            selected = isSelected,
                            onClick = { onSelectQuery(net.name, query.nick) },
                            modifier = Modifier
                                .padding(start = 24.dp, end = 8.dp)
                                .combinedClickable(
                                    onClick = { onSelectQuery(net.name, query.nick) },
                                    onLongClick = { showMenu = true },
                                ),
                            icon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = DpOffset(72.dp, 0.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Close") },
                                onClick = { showMenu = false; onCloseQuery(net.name, query.nick) },
                            )
                        }
                    }
                }
            }
        }
    }
}
