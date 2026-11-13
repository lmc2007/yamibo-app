package me.thenano.yamibo.yamibo_app.profile.sign

/** Limits silent attempts without suppressing retries for the rest of the day. */
internal class AutomaticSignInPolicy {
    private var activeKey: String? = null
    private var lastAttemptKey: String? = null
    private var lastAttemptAt = 0L

    fun tryStart(key: String?, enabled: Boolean, signedToday: Boolean?, nowMillis: Long): Boolean {
        if (!enabled || key == null || signedToday == true || activeKey != null) return false
        if (lastAttemptKey == key && nowMillis >= lastAttemptAt &&
            nowMillis - lastAttemptAt < RETRY_INTERVAL_MILLIS
        ) return false
        activeKey = key
        lastAttemptKey = key
        lastAttemptAt = nowMillis
        return true
    }

    fun finish(key: String) {
        if (activeKey == key) activeKey = null
    }

    fun reset() {
        activeKey = null
        lastAttemptKey = null
    }

    companion object {
        const val RETRY_INTERVAL_MILLIS = 5 * 60_000L
    }
}
