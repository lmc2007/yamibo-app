package me.thenano.yamibo.yamibo_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.favorite.sync.AndroidAppForegroundTracker
import me.thenano.yamibo.yamibo_app.favorite.sync.AndroidBackgroundTaskRepository
import me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.rememberRestorableNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.access.AndroidBackgroundAccessRepository
import me.thenano.yamibo.yamibo_app.repository.*
import me.thenano.yamibo.yamibo_app.repository.favorite.FavoriteSyncRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.store.AndroidCookieStore
import me.thenano.yamibo.yamibo_app.store.AndroidUserStore
import me.thenano.yamibo.yamibo_app.store.settings.AndroidSettingsStore

class MainActivity : ComponentActivity() {
    var lastBackTime = 0L

    override fun onStart() {
        super.onStart()
        AndroidAppForegroundTracker.markForeground(true)
    }

    override fun onStop() {
        AndroidAppForegroundTracker.markForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { }

            /** Navigator Logic */
            val navigator = rememberRestorableNavigator()
            DisposableEffect(navigator) {
                val callback = onBackPressedDispatcher.addCallback(this@MainActivity) {
                    val exitInterval = 2000L // 2 seconds
                    if (navigator.dispatchBack()) return@addCallback

                    val now = System.currentTimeMillis()
                    if (now - lastBackTime < exitInterval) {
                        Logger.i(
                            "AndroidBackHandler",
                            "Double Tapped(Interval=${now - lastBackTime}) , Exit."
                        )
                        finish()
                    } else {
                        lastBackTime = now
                        Toast.makeText(this@MainActivity, "再按一次退出應用", Toast.LENGTH_SHORT).show()
                    }
                }
                onDispose { callback.remove() }
            }
            /** Store Logic */
            val cookieStore = remember { AndroidCookieStore(context) }
            val userStore = remember { AndroidUserStore(context) }
            val settingsStore = remember { AndroidSettingsStore(context) }
            val appSettingsRepository = remember { AppSettingsRepository(settingsStore) }
            val novelReaderSettingsRepository = remember { NovelReaderSettingsRepository(settingsStore) }
            val mangaReaderSettingsRepository = remember { MangaReaderSettingsRepository(settingsStore) }

            /** Repository Logic */
            val yamiboClient = remember { YamiboClient(timeoutMillis = 60_000L) }
            val authRepository = remember {
                AndroidAuthRepository(cookieStore, userStore, yamiboClient)
            }
            val dbFactory = remember { DatabaseFactory(context) }
            val diskCacheFactory = remember {
                DiskCacheFactory(
                    dbFactory,
                    cacheDirPath = context.cacheDir.absolutePath
                )
            }
            
            val forumRepository = remember { AndroidForumRepository(cookieStore, yamiboClient, diskCacheFactory) }
            val threadRepository = remember { AndroidThreadRepository(cookieStore, yamiboClient, diskCacheFactory) }
            val favoriteRepository = remember { AndroidLocalFavoriteRepository(dbFactory) }
            val remoteFavoriteRepository = remember { AndroidFavoriteRepository(cookieStore, yamiboClient) }
            val favoriteSyncDatabase = remember { Database(dbFactory.createDriver()) }
            val favoriteSyncRepository = remember {
                FavoriteSyncRepositoryImpl(
                    db = favoriteSyncDatabase,
                    authRepository = authRepository,
                    favoriteRepository = remoteFavoriteRepository,
                    localFavoriteRepository = favoriteRepository,
                    threadRepository = threadRepository,
                )
            }
            val backgroundTaskRepository = remember { AndroidBackgroundTaskRepository(context) }
            val favoriteSyncRunner = remember { FavoriteSyncRunner(favoriteSyncRepository, backgroundTaskRepository) }
            val backgroundAccessRepository = remember { AndroidBackgroundAccessRepository(context) }
            val novelCacheRepository = remember { AndroidNovelThreadCacheRepository(diskCacheFactory) }
            val readHistoryRepository = remember { AndroidReadHistoryRepository(dbFactory) }
            val signRepository = remember { AndroidSignRepository(dbFactory, authRepository, appSettingsRepository) }
            val themeRepository = remember { AndroidThemeRepository() }
            val tagRepository = remember { AndroidTagRepository(cookieStore, yamiboClient, diskCacheFactory) }

            /** Provide Repositories */
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalAuthRepository provides authRepository,
                LocalForumRepository provides forumRepository,
                LocalThreadRepository provides threadRepository,
                LocalFavoriteRepository provides favoriteRepository,
                LocalRemoteFavoriteRepository provides remoteFavoriteRepository,
                LocalFavoriteSyncRepository provides favoriteSyncRepository,
                LocalFavoriteSyncRunner provides favoriteSyncRunner,
                LocalBackgroundAccessRepository provides backgroundAccessRepository,
                LocalNovelThreadCacheRepository provides novelCacheRepository,
                LocalReadHistoryRepository provides readHistoryRepository,
                LocalSignRepository provides signRepository,
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
                
                LaunchedEffect(Unit) {
                    if (appSettingsRepository.clearCacheOnAppLaunch.getValue()) {
                        diskCacheFactory.clearAllCache()
                    }
                }
                LaunchedEffect(Unit) {
                    if (
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                App()
            }
        }
    }
}
