package com.lipapp

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lipapp.data.api.SseEventBus
import com.lipapp.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LipApp : Application(), Configuration.Provider {

    @Inject lateinit var eventBus: SseEventBus
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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
