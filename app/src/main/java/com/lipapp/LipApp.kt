package com.lipapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.lipapp.util.NotificationHelper
import com.lipapp.SseService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LipApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationHelper.CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "IRC message notifications" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                SseService.CHANNEL_ID,
                "Connection",
                NotificationManager.IMPORTANCE_MIN,
            ).apply { description = "Persistent connection status" }
        )
    }
}
