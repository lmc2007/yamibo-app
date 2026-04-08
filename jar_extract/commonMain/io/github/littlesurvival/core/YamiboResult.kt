package io.github.littlesurvival.core

sealed class YamiboResult<out T> {
    abstract fun message(): String

    data class Success<T>(val value: T) : YamiboResult<T>() {
        override fun message(): String = value.toString()
    }
    data class Failure(val reason: String, val exception: Throwable? = null)
        : YamiboResult<Nothing>() {
        override fun message(): String = reason
    }

    /** The website is currently under maintenance (HTTP 503 or maintenance HTML detected). */
    data object Maintenance : YamiboResult<Nothing>() {
        override fun message(): String = "又到了論壇備份的時間了，大家來杯紅茶休息三十分鐘吧"
    }

    /** The user is not logged in or their session has expired. */
    data object NotLoggedIn : YamiboResult<Nothing>() {
        override fun message(): String = "抱歉，您尚未登录，没有权限访问该版块"
    }

    data class NoPermission(val reason: String) : YamiboResult<Nothing>() {
        override fun message(): String = reason
    }
}
