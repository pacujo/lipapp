package com.lipapp.data.api

import com.lipapp.data.model.TokenRequest
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? {
        val username = tokenManager.username
        val password = tokenManager.password
        if (username.isEmpty() || password.isEmpty()) return null

        if (responseCount(response) >= 2) return null

        val baseUrl = tokenManager.baseUrl.trimEnd('/')
        val body = json.encodeToString(TokenRequest.serializer(), TokenRequest(username, password))
            .toRequestBody("application/json".toMediaType())

        val tokenRequest = Request.Builder()
            .url("$baseUrl/auth/token")
            .post(body)
            .build()

        val tokenResponse = OkHttpClient().newCall(tokenRequest).execute()
        if (!tokenResponse.isSuccessful) return null

        val responseBody = tokenResponse.body?.string() ?: return null
        val tokenData = json.decodeFromString<com.lipapp.data.model.TokenResponse>(responseBody)
        tokenManager.token = tokenData.token

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${tokenData.token}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
