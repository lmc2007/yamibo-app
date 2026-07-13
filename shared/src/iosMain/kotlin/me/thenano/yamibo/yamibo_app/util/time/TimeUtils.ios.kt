package me.thenano.yamibo.yamibo_app.util.time

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
