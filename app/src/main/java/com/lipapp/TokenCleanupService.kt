package com.lipapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lipapp.data.api.TokenManager
import com.lipapp.data.prefs.PollPointerStore
import com.lipapp.work.PollWorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class TokenCleanupService : Service() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var pollWorkScheduler: PollWorkScheduler
    @Inject lateinit var pollPointerStore: PollPointerStore

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopService(Intent(this, SseService::class.java))
        pollWorkScheduler.cancel()
        runBlocking { pollPointerStore.clear() }
        tokenManager.clear()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
