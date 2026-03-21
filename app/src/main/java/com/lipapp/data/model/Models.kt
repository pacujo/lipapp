package com.lipapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    val username: String,
    val password: String,
)

@Serializable
data class TokenResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class NetworkCreate(
    val name: String,
    val host: String,
    val port: Int = 6697,
    val tls: Boolean = true,
    val nick: String,
    @SerialName("nickserv_password") val nickservPassword: String? = null,
)

@Serializable
data class NetworkResponse(
    val name: String,
    val host: String,
    val port: Int,
    val tls: Boolean,
    val nick: String,
    val state: String,
)

@Serializable
data class ChannelResponse(
    val name: String,
    val joined: Boolean = false,
    val topic: String? = null,
)

@Serializable
data class JoinRequest(
    val name: String,
    val key: String? = null,
)

@Serializable
data class Message(
    val id: String,
    val time: String,
    val from: String,
    val type: String,
    val text: String,
)

@Serializable
data class MessagePage(
    val messages: List<Message>,
    @SerialName("has_more") val hasMore: Boolean = false,
)

@Serializable
data class SendMessage(
    val text: String,
    val type: String = "privmsg",
)

@Serializable
data class QueryResponse(
    val nick: String,
)

@Serializable
data class Session(
    @SerialName("current_network") val currentNetwork: String? = null,
    @SerialName("current_channel") val currentChannel: String? = null,
    @SerialName("current_query") val currentQuery: String? = null,
    val pointers: Map<String, String> = emptyMap(),
)

@Serializable
data class SessionUpdate(
    @SerialName("current_network") val currentNetwork: String? = null,
    @SerialName("current_channel") val currentChannel: String? = null,
    @SerialName("current_query") val currentQuery: String? = null,
    val pointers: Map<String, String>? = null,
)

@Serializable
data class MemberResponse(
    val nick: String,
    val prefix: String = "",
    val user: String? = null,
    val host: String? = null,
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
)

data class SseEvent(
    val event: String,
    val data: String,
    val id: String? = null,
)

@Serializable
data class MessageEvent(
    val network: String,
    val channel: String? = null,
    val nick: String? = null,
    val id: String,
    val time: String,
    val from: String,
    val type: String,
    val text: String,
)

@Serializable
data class NetworkStateEvent(
    val network: String,
    val state: String,
)

@Serializable
data class NickEvent(
    val network: String,
    @SerialName("old_nick") val oldNick: String,
    @SerialName("new_nick") val newNick: String,
)

@Serializable
data class JoinPartEvent(
    val network: String,
    val channel: String,
    val nick: String,
    val message: String? = null,
)

@Serializable
data class KickEvent(
    val network: String,
    val channel: String,
    val nick: String,
    val by: String,
    val message: String? = null,
)

sealed class ChatTarget {
    abstract val network: String
    abstract val displayName: String

    data class Channel(override val network: String, val name: String) : ChatTarget() {
        override val displayName get() = name
    }

    data class Query(override val network: String, val nick: String) : ChatTarget() {
        override val displayName get() = nick
    }

    val key: String
        get() = when (this) {
            is Channel -> "$network/$name"
            is Query -> "$network/$nick"
        }
}

data class SidebarItem(
    val network: NetworkResponse,
    val channels: List<ChannelResponse>,
    val queries: List<QueryResponse>,
)
