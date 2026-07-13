package com.lipapp.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lipapp.data.api.SseEventBus
import com.lipapp.data.api.TokenManager
import com.lipapp.data.prefs.PollPointerStore
import com.lipapp.data.repository.LipserviceRepository
import com.lipapp.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MessagePollWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: LipserviceRepository,
    private val pollPointerStore: PollPointerStore,
    private val notificationHelper: NotificationHelper,
    private val eventBus: SseEventBus,
    private val tokenManager: TokenManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (tokenManager.token == null) return Result.success()
        if (eventBus.isInForeground.get()) return Result.success()

        return try {
            if (!pollPointerStore.isBootstrapped()) {
                val session = repository.getSession()
                pollPointerStore.bootstrap(session.pointers)
                return Result.success()
            }

            val pointers = pollPointerStore.getPointers()
            val response = repository.pollNotifications(pointers)
            if (response.items.isEmpty()) return Result.success()

            val pointerUpdates = mutableMapOf<String, String>()
            for (item in response.items) {
                pointerUpdates[item.key] = item.id
                if (item.type == "meta") continue
                notificationHelper.showMessageNotification(
                    item.network,
                    item.displayTarget,
                    item.from,
                    item.text,
                )
            }
            pollPointerStore.updatePointers(pointerUpdates)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
