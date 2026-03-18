package com.lipapp.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val baseUrl = tokenManager.baseUrl
        if (baseUrl.isNotEmpty()) {
            val path = request.url.encodedPath.trimStart('/')
            val query = request.url.encodedQuery
            val newUrl = "${baseUrl.trimEnd('/')}/$path" +
                if (query != null) "?$query" else ""
            request = request.newBuilder().url(newUrl).build()
        }

        val token = tokenManager.token
        if (token != null) {
            request = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
