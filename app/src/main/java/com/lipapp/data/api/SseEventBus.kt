package com.lipapp.data.api

import com.lipapp.data.model.SseEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<SseEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<SseEvent> = _events

    val currentTarget = AtomicReference<String?>(null)

    fun emit(event: SseEvent) {
        _events.tryEmit(event)
    }
}
