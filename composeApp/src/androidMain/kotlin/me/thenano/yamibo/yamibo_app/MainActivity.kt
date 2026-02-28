package me.thenano.yamibo.yamibo_app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.AndroidAuthRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidForumRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidThemeRepository
import me.thenano.yamibo.yamibo_app.store.AndroidCookieStore
import me.thenano.yamibo.yamibo_app.store.AndroidUserStore

class MainActivity : ComponentActivity() {
    var lastBackTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            /** Navigator Logic */
            val navigator = remember { ComposableNavigator() }
            onBackPressedDispatcher.addCallback(this) {
                val exitInterval = 2000L // 2 秒
                if (navigator.pop()) return@addCallback

                val now = System.currentTimeMillis()
                if (now - lastBackTime < exitInterval) {
                    Log.i(
                        __info__tag("AndroidBackHandler"),
                        "Double Tapped(Interval=${now - lastBackTime}) , Exit."
                    )
                    finish()
                } else {
                    lastBackTime = now
                    Toast.makeText(this@MainActivity, "再按一次退出應用", Toast.LENGTH_SHORT).show()
                }
            }
            /** Store Logic */
            val cookieStore = remember { AndroidCookieStore(context) }
            val userStore = remember { AndroidUserStore(context) }

            /** Repository Logic */
            val yamiboClient = remember { YamiboClient() }
            val authRepository = remember {
                AndroidAuthRepository(cookieStore, userStore, yamiboClient)
            }
            val forumRepository = remember { AndroidForumRepository(cookieStore, yamiboClient) }
            val themeRepository = remember { AndroidThemeRepository() }

            /** Provide Repositories */
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalAuthRepository provides authRepository,
                LocalForumRepository provides forumRepository,
                LocalThemeRepository provides themeRepository,
            ) { App() }
        }
    }
}
