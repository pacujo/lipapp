package com.lipapp

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import com.lipapp.data.api.SseEventBus
import com.lipapp.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LipApp : Application() {

    @Inject lateinit var eventBus: SseEventBus

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

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                eventBus.isInForeground.set(true)
            }
            override fun onActivityStopped(activity: Activity) {
                eventBus.isInForeground.set(false)
            }
            override fun onActivityCreated(activity: Activity, s: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, s: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
