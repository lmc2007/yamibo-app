package me.thenano.yamibo.yamibo_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.AndroidAuthRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidFavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidForumRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidThemeRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidThreadRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidNovelThreadCacheRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.AndroidTagRepository
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.store.AndroidCookieStore
import me.thenano.yamibo.yamibo_app.store.AndroidUserStore
import me.thenano.yamibo.yamibo_app.store.settings.AndroidSettingsStore
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository

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
                val exitInterval = 2000L // 2 seconds
                if (navigator.pop()) return@addCallback

                val now = System.currentTimeMillis()
                if (now - lastBackTime < exitInterval) {
                    me.thenano.yamibo.yamibo_app.Logger.i(
                        "AndroidBackHandler",
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
            val yamiboClient = remember { YamiboClient(timeoutMillis = 60_000L) }
            val authRepository = remember {
                AndroidAuthRepository(cookieStore, userStore, yamiboClient)
            }
            val dbFactory = remember { DatabaseFactory(context) }
            val diskCacheFactory = remember { me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory(dbFactory, cacheDirPath = context.cacheDir.absolutePath) }
            
            val forumRepository = remember { AndroidForumRepository(cookieStore, yamiboClient, diskCacheFactory) }
            val threadRepository = remember { AndroidThreadRepository(cookieStore, yamiboClient, diskCacheFactory) }
            val favoriteRepository = remember { AndroidFavoriteRepository(cookieStore, yamiboClient) }
            val novelCacheRepository = remember { AndroidNovelThreadCacheRepository(diskCacheFactory) }
            val readHistoryRepository = remember { AndroidReadHistoryRepository(dbFactory) }
            val themeRepository = remember { AndroidThemeRepository() }
            val tagRepository = remember { AndroidTagRepository(cookieStore, yamiboClient, diskCacheFactory) }
            val settingsStore = remember { AndroidSettingsStore(context) }
            val appSettingsRepository = remember { AppSettingsRepository(settingsStore) }
            val novelReaderSettingsRepository = remember { NovelReaderSettingsRepository(settingsStore) }
            val mangaReaderSettingsRepository = remember { MangaReaderSettingsRepository(settingsStore) }

            /** Provide Repositories */
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalAuthRepository provides authRepository,
                LocalForumRepository provides forumRepository,
                LocalThreadRepository provides threadRepository,
                LocalFavoriteRepository provides favoriteRepository,
                LocalNovelThreadCacheRepository provides novelCacheRepository,
                LocalReadHistoryRepository provides readHistoryRepository,
                LocalThemeRepository provides themeRepository,
                LocalTagRepository provides tagRepository,
                LocalAppSettingsRepository provides appSettingsRepository,
                LocalDiskCacheFactory provides diskCacheFactory,
                LocalNovelReaderSettingsRepository provides novelReaderSettingsRepository,
                LocalMangaReaderSettingsRepository provides mangaReaderSettingsRepository,
            ) {
                /** Color system bars to match active theme */
                val scheme = LocalThemeRepository.current.getColorScheme()
                SideEffect {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = scheme.brownDeep.toInt()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = scheme.brownDeep.toInt()
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = false
                        isAppearanceLightNavigationBars = false
                    }
                }
                
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (appSettingsRepository.clearCacheOnAppLaunch.getValue()) {
                        diskCacheFactory.clearAllCache()
                    }
                }

                App()
            }
        }
    }
}
