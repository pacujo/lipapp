package com.lipapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Kept so [TokenCleanupService] can stop it on task removal. SSE and
 * notifications are handled by [com.lipapp.ui.main.MainViewModel].
 */
@AndroidEntryPoint
class SseService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY
}
