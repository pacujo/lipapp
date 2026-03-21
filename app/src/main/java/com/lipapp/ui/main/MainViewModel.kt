package com.lipapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lipapp.data.model.*
import com.lipapp.data.prefs.AppPreferences
import com.lipapp.data.repository.LipserviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class MainUiState(
    val sidebar: List<SidebarItem> = emptyList(),
    val currentTarget: ChatTarget? = null,
    val messages: List<Message> = emptyList(),
    val hasMoreMessages: Boolean = false,
    val unread: Set<String> = emptySet(),
    val myNicks: Map<String, String> = emptyMap(),
    val isLoadingMessages: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val darkMode: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<Message>? = null,
    val pointers: MutableMap<String, String> = mutableMapOf(),
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: LipserviceRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private var sseJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.darkMode.collect { dark ->
                _state.update { it.copy(darkMode = dark) }
            }
        }
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val networks = repository.getNetworks()
                val sidebarItems = networks.map { net ->
                    val channels = try { repository.getChannels(net.name) } catch (_: Exception) { emptyList() }
                    val queries = try { repository.getQueries(net.name) } catch (_: Exception) { emptyList() }
                    SidebarItem(net, channels, queries)
                }

                val nicks = networks.associate { it.name to it.nick }

                _state.update { it.copy(sidebar = sidebarItems, myNicks = nicks) }

                try {
                    val session = repository.getSession()
                    _state.update { it.copy(pointers = session.pointers.toMutableMap()) }
                    if (session.currentNetwork != null) {
                        if (session.currentChannel != null) {
                            selectTarget(ChatTarget.Channel(session.currentNetwork, session.currentChannel))
                        } else if (session.currentQuery != null) {
                            selectTarget(ChatTarget.Query(session.currentNetwork, session.currentQuery))
                        }
                    }
                } catch (_: Exception) { }

                startSseConnection()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun selectTarget(target: ChatTarget) {
        _state.update {
            it.copy(
                currentTarget = target,
                messages = emptyList(),
                hasMoreMessages = false,
                isLoadingMessages = true,
                unread = it.unread - target.key,
                searchQuery = "",
                searchResults = null,
                isSearching = false,
            )
        }

        viewModelScope.launch {
            try {
                val page = when (target) {
                    is ChatTarget.Channel ->
                        repository.getChannelMessages(target.network, target.name, limit = 100)
                    is ChatTarget.Query ->
                        repository.getPrivateMessages(target.network, target.nick, limit = 100)
                }
                _state.update {
                    it.copy(
                        messages = page.messages,
                        hasMoreMessages = page.hasMore,
                        isLoadingMessages = false,
                    )
                }
                if (page.messages.isNotEmpty()) {
                    updatePointer(target.key, page.messages.last().id)
                }
                saveSession()
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMessages = false, error = e.message) }
            }
        }
    }

    fun loadMoreMessages() {
        val target = _state.value.currentTarget ?: return
        val messages = _state.value.messages
        if (messages.isEmpty() || _state.value.isLoadingMore || !_state.value.hasMoreMessages) return

        _state.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            try {
                val firstId = messages.first().id
                val page = when (target) {
                    is ChatTarget.Channel ->
                        repository.getChannelMessages(target.network, target.name, limit = 100, before = firstId)
                    is ChatTarget.Query ->
                        repository.getPrivateMessages(target.network, target.nick, limit = 100, before = firstId)
                }
                _state.update {
                    it.copy(
                        messages = page.messages + it.messages,
                        hasMoreMessages = page.hasMore,
                        isLoadingMore = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMore = false, error = e.message) }
            }
        }
    }

    fun sendMessage(text: String) {
        val target = _state.value.currentTarget ?: return
        if (text.isBlank()) return

        val msgType = if (text.startsWith("/me ")) "action" else "privmsg"
        val msgText = if (msgType == "action") text.removePrefix("/me ") else text

        viewModelScope.launch {
            try {
                when (target) {
                    is ChatTarget.Channel ->
                        repository.sendChannelMessage(target.network, target.name, msgText, msgType)
                    is ChatTarget.Query ->
                        repository.sendPrivateMessage(target.network, target.nick, msgText, msgType)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun addNetwork(name: String, host: String, port: Int, nick: String, password: String?, tls: Boolean) {
        viewModelScope.launch {
            try {
                repository.createNetwork(
                    NetworkCreate(
                        name = name, host = host, port = port,
                        tls = tls, nick = nick,
                        nickservPassword = password?.ifBlank { null },
                    )
                )
                repository.connectNetwork(name)
                reloadSidebar()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun joinChannel(network: String, channel: String) {
        val name = if (channel.startsWith("#")) channel else "#$channel"
        viewModelScope.launch {
            try {
                repository.joinChannel(network, name)
                reloadSidebar()
                selectTarget(ChatTarget.Channel(network, name))
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun leaveChannel(network: String, channel: String) {
        viewModelScope.launch {
            try {
                repository.leaveChannel(network, channel)
                if (_state.value.currentTarget == ChatTarget.Channel(network, channel)) {
                    _state.update { it.copy(currentTarget = null, messages = emptyList()) }
                }
                reloadSidebar()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun closeQuery(network: String, nick: String) {
        viewModelScope.launch {
            try {
                repository.closeQuery(network, nick)
                if (_state.value.currentTarget == ChatTarget.Query(network, nick)) {
                    _state.update { it.copy(currentTarget = null, messages = emptyList()) }
                }
                reloadSidebar()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun connectNetwork(name: String) {
        viewModelScope.launch {
            try {
                repository.connectNetwork(name)
                reloadSidebar()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun disconnectNetwork(name: String) {
        viewModelScope.launch {
            try {
                repository.disconnectNetwork(name)
                reloadSidebar()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteNetwork(name: String) {
        viewModelScope.launch {
            try {
                repository.deleteNetwork(name)
                if (_state.value.currentTarget?.network == name) {
                    _state.update { it.copy(currentTarget = null, messages = emptyList()) }
                }
                reloadSidebar()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newMode = !_state.value.darkMode
            prefs.setDarkMode(newMode)
        }
    }

    fun search(query: String) {
        val target = _state.value.currentTarget ?: return
        if (query.isBlank()) {
            _state.update { it.copy(searchQuery = "", searchResults = null, isSearching = false) }
            return
        }

        _state.update { it.copy(searchQuery = query, isSearching = true) }

        viewModelScope.launch {
            try {
                val page = when (target) {
                    is ChatTarget.Channel ->
                        repository.searchChannelMessages(target.network, target.name, query)
                    is ChatTarget.Query ->
                        repository.searchPrivateMessages(target.network, target.nick, query)
                }
                _state.update { it.copy(searchResults = page.messages, isSearching = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isSearching = false, error = e.message) }
            }
        }
    }

    fun clearSearch() {
        _state.update { it.copy(searchQuery = "", searchResults = null, isSearching = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun logout() {
        sseJob?.cancel()
        repository.logout()
    }

    private suspend fun reloadSidebar() {
        try {
            val networks = repository.getNetworks()
            val sidebarItems = networks.map { net ->
                val channels = try { repository.getChannels(net.name) } catch (_: Exception) { emptyList() }
                val queries = try { repository.getQueries(net.name) } catch (_: Exception) { emptyList() }
                SidebarItem(net, channels, queries)
            }
            val nicks = networks.associate { it.name to it.nick }
            _state.update { it.copy(sidebar = sidebarItems, myNicks = nicks) }
        } catch (_: Exception) { }
    }

    private fun startSseConnection() {
        sseJob?.cancel()
        sseJob = viewModelScope.launch {
            var backoff = 1000L
            while (isActive) {
                try {
                    repository.connectSse().collect { event ->
                        handleSseEvent(event)
                        backoff = 1000L
                    }
                } catch (_: Exception) {
                    if (isActive) {
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(30_000L)
                        reloadSidebar()
                    }
                }
            }
        }
    }

    private fun handleSseEvent(event: SseEvent) {
        when (event.event) {
            "message" -> {
                val msg = json.decodeFromString<MessageEvent>(event.data)
                val targetKey = if (msg.channel != null) "${msg.network}/${msg.channel}"
                    else "${msg.network}/${msg.nick}"
                val currentKey = _state.value.currentTarget?.key

                val message = Message(
                    id = msg.id, time = msg.time,
                    from = msg.from, type = msg.type, text = msg.text,
                )

                if (targetKey == currentKey) {
                    _state.update { it.copy(messages = it.messages + message) }
                    updatePointer(targetKey, msg.id)
                } else {
                    _state.update { it.copy(unread = it.unread + targetKey) }
                }
            }
            "network_state" -> {
                val ev = json.decodeFromString<NetworkStateEvent>(event.data)
                _state.update { state ->
                    state.copy(
                        sidebar = state.sidebar.map { item ->
                            if (item.network.name == ev.network)
                                item.copy(network = item.network.copy(state = ev.state))
                            else item
                        }
                    )
                }
                if (ev.state == "connected") {
                    viewModelScope.launch { reloadSidebar() }
                }
            }
            "nick" -> {
                val ev = json.decodeFromString<NickEvent>(event.data)
                val myNick = _state.value.myNicks[ev.network]
                if (ev.oldNick == myNick) {
                    _state.update {
                        it.copy(myNicks = it.myNicks + (ev.network to ev.newNick))
                    }
                }
            }
            "join", "part", "kick" -> {
                viewModelScope.launch { reloadSidebar() }
            }
        }
    }

    private fun updatePointer(key: String, messageId: String) {
        _state.value.pointers[key] = messageId
    }

    private fun saveSession() {
        val target = _state.value.currentTarget ?: return
        viewModelScope.launch {
            try {
                repository.updateSession(
                    SessionUpdate(
                        currentNetwork = target.network,
                        currentChannel = (target as? ChatTarget.Channel)?.name,
                        currentQuery = (target as? ChatTarget.Query)?.nick,
                        pointers = _state.value.pointers.toMap(),
                    )
                )
            } catch (_: Exception) { }
        }
    }
}
