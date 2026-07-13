package me.thenano.yamibo.yamibo_app.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>()
    val events = _events.asSharedFlow()

    suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }
}
