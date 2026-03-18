package com.lipapp.data.api

import com.lipapp.data.model.SseEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
) {
    fun connect(baseUrl: String): Flow<SseEvent> = callbackFlow {
        val url = "${baseUrl.trimEnd('/')}/events"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Authorization", "Bearer ${tokenManager.token}")
            .build()

        val call = okHttpClient.newCall(request)

        val job = launch(Dispatchers.IO) {
            val response = call.execute()
            val body = response.body ?: return@launch
            val reader = BufferedReader(InputStreamReader(body.byteStream()))

            try {
                var event = ""
                var data = StringBuilder()
                var id: String? = null

                while (isActive) {
                    val line = reader.readLine() ?: break

                    when {
                        line.startsWith("event:") ->
                            event = line.substring(6).trim()
                        line.startsWith("data:") -> {
                            if (data.isNotEmpty()) data.append('\n')
                            data.append(line.substring(5).trim())
                        }
                        line.startsWith("id:") ->
                            id = line.substring(3).trim()
                        line.isEmpty() && event.isNotEmpty() -> {
                            if (data.isNotEmpty()) {
                                trySend(SseEvent(event, data.toString(), id))
                            }
                            event = ""
                            data = StringBuilder()
                            id = null
                        }
                        line.startsWith(":") -> { /* SSE comment / keepalive */ }
                    }
                }
            } finally {
                reader.close()
                response.close()
            }
        }

        awaitClose {
            call.cancel()
            job.cancel()
        }
    }
}
