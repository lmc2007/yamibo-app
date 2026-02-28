package me.thenano.yamibo.yamibo_app.repository

import android.webkit.CookieManager
import io.github.littlesurvival.YamiboClient
import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.ProfilePage
import kotlinx.coroutines.delay
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore
import me.thenano.yamibo.yamibo_app.store.auth.UserStore
import me.thenano.yamibo.yamibo_app.util.auth.parseCookieStringToMap

class AndroidAuthRepository(
        override val cookieStore: CookieStore,
        override val userStore: UserStore,
        override val yamiboClient: YamiboClient
) : AuthRepository {
    override suspend fun isLoggedIn(): Boolean {
        return parseCookieStringToMap(cookieStore.load()).containsKey(authCookieKey)
    }

    override suspend fun fetchStatus(): YamiboResult<Boolean> {
        if (!isLoggedIn()) return YamiboResult.Failure("查無登入資料，請重新登入")

        yamiboClient.setCookie(cookieStore.load() ?: "")
        val profileResult = yamiboClient.fetchProfileInfo()

        when (profileResult) {
            is YamiboResult.Success -> {
                userStore.save(profileResult.value)
                return YamiboResult.Success(true)
            }
            is YamiboResult.NotLoggedIn -> {
                logOut()
                return YamiboResult.Failure("登入資訊過期，請重新登入")
            }
            is YamiboResult.Maintenance -> {
                return YamiboResult.Failure("伺服器正在維護中")
            }
            is YamiboResult.Failure -> {
                return YamiboResult.Failure("獲取用戶資料失敗: ${profileResult.reason}")
            }
        }
    }

    override suspend fun startLoginDetect(onSuccess: suspend () -> Unit, onTimeOut: () -> Unit) {
        var elapsed = 0L

        while (elapsed < loginTimeout) {
            if (isLoggedIn()) {
                onSuccess()
                return
            }
            delay(loginDetectInterval)
            elapsed += loginDetectInterval
        }

        onTimeOut()
    }

    override fun syncCookieFromWebView() {
        val cookie = CookieManager.getInstance().getCookie(YamiboRoute.Domain.build()) ?: return
        cookieStore.save(cookie)
    }

    override fun currentUser(): ProfilePage? {
        return userStore.load()
    }

    override suspend fun logOut() {
        cookieStore.clear()
        userStore.clear()
        /** remove cookie from webview */
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
    }
}
