package me.thenano.yamibo.yamibo_app.favorite.sync

import java.util.concurrent.atomic.AtomicBoolean

object AndroidAppForegroundTracker {
    private val inForeground = AtomicBoolean(false)

    fun markForeground(isForeground: Boolean) {
        inForeground.set(isForeground)
    }

    fun isForeground(): Boolean = inForeground.get()
}
