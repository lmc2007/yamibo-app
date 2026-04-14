package me.thenano.yamibo.yamibo_app.util.time

import io.github.littlesurvival.dto.model.TimeInfo

fun TimeInfo?.epochMillisOrNull(): Long? {
    val epochSeconds = this?.epoch ?: return null
    if (epochSeconds <= 0L) return null
    return epochSeconds * 1000L
}
