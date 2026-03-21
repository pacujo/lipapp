package com.lipapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lipapp.data.api.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TokenCleanupService : Service() {

    @Inject lateinit var tokenManager: TokenManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        tokenManager.clear()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
