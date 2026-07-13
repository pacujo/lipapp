package com.lipapp.data.repository

import com.lipapp.data.api.LipserviceApi
import com.lipapp.data.api.SseClient
import com.lipapp.data.api.TokenManager
import com.lipapp.data.model.*
import com.lipapp.data.prefs.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LipserviceRepository @Inject constructor(
    private val api: LipserviceApi,
    private val sseClient: SseClient,
    private val tokenManager: TokenManager,
    private val prefs: AppPreferences,
) {
    val isLoggedIn: Boolean get() = tokenManager.token != null

    suspend fun login(url: String, username: String, password: String): TokenResponse {
        tokenManager.baseUrl = url
        tokenManager.username = username
        tokenManager.password = password
        val response = api.login(TokenRequest(username, password))
        tokenManager.token = response.token
        prefs.saveLoginInfo(url, username)
        return response
    }

    fun logout() {
        tokenManager.token = null
    }

    fun loginSync(username: String, password: String): TokenResponse =
        runBlocking { api.login(TokenRequest(username, password)) }

    fun connectSse(): Flow<SseEvent> = sseClient.connect(tokenManager.baseUrl)

    suspend fun getNetworks() = api.getNetworks()
    suspend fun createNetwork(network: NetworkCreate) = api.createNetwork(network)
    suspend fun deleteNetwork(name: String) = api.deleteNetwork(name)
    suspend fun connectNetwork(name: String) = api.connectNetwork(name)
    suspend fun disconnectNetwork(name: String) = api.disconnectNetwork(name)

    suspend fun getChannels(network: String) = api.getChannels(network)
    suspend fun joinChannel(network: String, name: String, key: String? = null) =
        api.joinChannel(network, JoinRequest(name, key))
    suspend fun leaveChannel(network: String, channel: String) =
        api.leaveChannel(network, channel)

    suspend fun getChannelMessages(
        network: String, channel: String,
        limit: Int? = null, before: String? = null,
        after: String? = null, around: String? = null,
    ) = api.getChannelMessages(network, channel, limit, before, after, around)

    suspend fun sendChannelMessage(
        network: String, channel: String, text: String, type: String = "privmsg",
    ) = api.sendChannelMessage(network, channel, SendMessage(text, type))

    suspend fun searchChannelMessages(
        network: String, channel: String, query: String,
        limit: Int? = null, anchor: String? = null, direction: String? = null,
    ) = api.searchChannelMessages(network, channel, query, limit, anchor, direction)

    suspend fun getMembers(network: String, channel: String) =
        api.getMembers(network, channel)

    suspend fun getQueries(network: String) = api.getQueries(network)

    suspend fun getPrivateMessages(
        network: String, nick: String,
        limit: Int? = null, before: String? = null,
        after: String? = null, around: String? = null,
    ) = api.getPrivateMessages(network, nick, limit, before, after, around)

    suspend fun sendPrivateMessage(
        network: String, nick: String, text: String, type: String = "privmsg",
    ) = api.sendPrivateMessage(network, nick, SendMessage(text, type))

    suspend fun closeQuery(network: String, nick: String) =
        api.closeQuery(network, nick)

    suspend fun searchPrivateMessages(
        network: String, nick: String, query: String,
        limit: Int? = null, anchor: String? = null, direction: String? = null,
    ) = api.searchPrivateMessages(network, nick, query, limit, anchor, direction)

    suspend fun getSession() = api.getSession()
    suspend fun updateSession(session: SessionUpdate) = api.updateSession(session)

    suspend fun pollNotifications(pointers: Map<String, String>) =
        api.pollNotifications(PollRequest(pointers))
}
