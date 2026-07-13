package com.lipapp.data.api

import com.lipapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface LipserviceApi {

    @POST("auth/token")
    suspend fun login(@Body request: TokenRequest): TokenResponse

    @DELETE("auth/token")
    suspend fun logout(): Response<Unit>

    @GET("networks")
    suspend fun getNetworks(): List<NetworkResponse>

    @POST("networks")
    suspend fun createNetwork(@Body network: NetworkCreate): NetworkResponse

    @GET("networks/{name}")
    suspend fun getNetwork(@Path("name") name: String): NetworkResponse

    @DELETE("networks/{name}")
    suspend fun deleteNetwork(@Path("name") name: String): Response<Unit>

    @POST("networks/{name}/connect")
    suspend fun connectNetwork(@Path("name") name: String): NetworkResponse

    @POST("networks/{name}/disconnect")
    suspend fun disconnectNetwork(@Path("name") name: String): NetworkResponse

    @GET("networks/{net}/channels")
    suspend fun getChannels(@Path("net") network: String): List<ChannelResponse>

    @POST("networks/{net}/channels")
    suspend fun joinChannel(
        @Path("net") network: String,
        @Body request: JoinRequest,
    ): ChannelResponse

    @DELETE("networks/{net}/channels/{channel}")
    suspend fun leaveChannel(
        @Path("net") network: String,
        @Path("channel") channel: String,
    ): Response<Unit>

    @GET("networks/{net}/channels/{channel}/messages")
    suspend fun getChannelMessages(
        @Path("net") network: String,
        @Path("channel") channel: String,
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null,
        @Query("after") after: String? = null,
        @Query("around") around: String? = null,
    ): MessagePage

    @POST("networks/{net}/channels/{channel}/messages")
    suspend fun sendChannelMessage(
        @Path("net") network: String,
        @Path("channel") channel: String,
        @Body message: SendMessage,
    ): Message

    @GET("networks/{net}/channels/{channel}/messages/search")
    suspend fun searchChannelMessages(
        @Path("net") network: String,
        @Path("channel") channel: String,
        @Query("q") query: String,
        @Query("limit") limit: Int? = null,
        @Query("anchor") anchor: String? = null,
        @Query("direction") direction: String? = null,
    ): MessagePage

    @GET("networks/{net}/channels/{channel}/members")
    suspend fun getMembers(
        @Path("net") network: String,
        @Path("channel") channel: String,
    ): List<MemberResponse>

    @GET("networks/{net}/queries")
    suspend fun getQueries(@Path("net") network: String): List<QueryResponse>

    @GET("networks/{net}/messages/{nick}")
    suspend fun getPrivateMessages(
        @Path("net") network: String,
        @Path("nick") nick: String,
        @Query("limit") limit: Int? = null,
        @Query("before") before: String? = null,
        @Query("after") after: String? = null,
        @Query("around") around: String? = null,
    ): MessagePage

    @POST("networks/{net}/messages/{nick}")
    suspend fun sendPrivateMessage(
        @Path("net") network: String,
        @Path("nick") nick: String,
        @Body message: SendMessage,
    ): Message

    @DELETE("networks/{net}/messages/{nick}")
    suspend fun closeQuery(
        @Path("net") network: String,
        @Path("nick") nick: String,
    ): Response<Unit>

    @GET("networks/{net}/messages/{nick}/search")
    suspend fun searchPrivateMessages(
        @Path("net") network: String,
        @Path("nick") nick: String,
        @Query("q") query: String,
        @Query("limit") limit: Int? = null,
        @Query("anchor") anchor: String? = null,
        @Query("direction") direction: String? = null,
    ): MessagePage

    @GET("session")
    suspend fun getSession(): Session

    @PUT("session")
    suspend fun updateSession(@Body session: SessionUpdate): Response<Unit>

    @POST("notifications/poll")
    suspend fun pollNotifications(@Body request: PollRequest): PollResponse
}
