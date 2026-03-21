package com.lipapp

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lipapp.data.api.SseClient
import com.lipapp.data.api.SseEventBus
import com.lipapp.data.api.TokenManager
import com.lipapp.data.model.MessageEvent
import com.lipapp.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class SseService : Service() {

    @Inject lateinit var sseClient: SseClient
    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var eventBus: SseEventBus
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private var sseJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    companion object {
        const val CHANNEL_ID = "connection"
        const val NOTIFICATION_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startSseConnection()
        registerNetworkCallback()
        return START_STICKY
    }

    override fun onDestroy() {
        sseJob?.cancel()
        scope.cancel()
        unregisterNetworkCallback()
        super.onDestroy()
    }

    private fun buildForegroundNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Lip")
            .setContentText("Connected")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun startSseConnection() {
        sseJob?.cancel()
        val baseUrl = tokenManager.baseUrl
        if (baseUrl.isEmpty() || tokenManager.token == null) return

        sseJob = scope.launch {
            var backoff = 1000L
            while (isActive) {
                try {
                    sseClient.connect(baseUrl).collect { event ->
                        eventBus.emit(event)
                        handleNotification(event)
                        backoff = 1000L
                    }
                } catch (_: Exception) {
                    if (isActive) {
                        delay(backoff)
                        backoff = (backoff * 2).coerceAtMost(30_000L)
                    }
                }
            }
        }
    }

    private fun handleNotification(event: com.lipapp.data.model.SseEvent) {
        if (event.event != "message") return
        try {
            val msg = json.decodeFromString<MessageEvent>(event.data)
            val target = msg.channel ?: msg.nick ?: return
            val targetKey = "${msg.network}/$target"
            if (targetKey == eventBus.currentTarget.get() && eventBus.isInForeground.get()) return
            notificationHelper.showMessageNotification(msg.network, target, msg.from, msg.text)
        } catch (_: Exception) {}
    }

    private fun registerNetworkCallback() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                startSseConnection()
            }

            override fun onLost(network: Network) {
                sseJob?.cancel()
            }
        }
        cm.registerNetworkCallback(request, callback)
        networkCallback = callback
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(it)
        }
        networkCallback = null
    }
}
