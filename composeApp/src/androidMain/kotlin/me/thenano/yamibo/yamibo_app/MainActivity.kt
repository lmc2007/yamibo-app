package me.thenano.yamibo.yamibo_app

import me.thenano.yamibo.yamibo_app.i18n.i18n

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.favorite.sync.AndroidAppForegroundTracker
import me.thenano.yamibo.yamibo_app.favorite.sync.AndroidBackgroundTaskRepository
import me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.favorite.updates.AndroidFavoriteUpdateScheduler
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.rememberRestorableNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.access.AndroidBackgroundAccessRepository
import me.thenano.yamibo.yamibo_app.profile.settings.backup.AndroidBackupScheduler
import me.thenano.yamibo.yamibo_app.profile.settings.sign.AndroidSignReminderScheduler
import me.thenano.yamibo.yamibo_app.profile.settings.sign.SignReminderScheduler
import me.thenano.yamibo.yamibo_app.util.SignReminderTrigger
import me.thenano.yamibo.yamibo_app.repository.*
import me.thenano.yamibo.yamibo_app.repository.backup.BackupRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.chineseconversion.createChineseConversionRepository
import me.thenano.yamibo.yamibo_app.repository.favorite.FavoriteSyncRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.favorite.FavoriteUpdateRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.font.AndroidFontPlatform
import me.thenano.yamibo.yamibo_app.repository.font.DefaultFontRepository
import me.thenano.yamibo.yamibo_app.repository.appupdate.DefaultAppUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.inapplinknavigation.DefaultInAppLinkNavigationRepository
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.userspace.BlogRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.userspace.UserSpaceRepositoryImpl
import me.thenano.yamibo.yamibo_app.store.AndroidCookieStore
import me.thenano.yamibo.yamibo_app.store.AndroidUserStore
import me.thenano.yamibo.yamibo_app.store.settings.AndroidSettingsStore
import me.thenano.yamibo.yamibo_app.update.AndroidAppUpdatePlatform
import me.thenano.yamibo.yamibo_app.util.state
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    var lastBackTime = 0L

    override fun onStart() {
        super.onStart()
        AndroidAppForegroundTracker.markForeground(true)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(EXTRA_FROM_NOTIFICATION_SIGN_IN, false) == true) {
            SignReminderTrigger.showSignWebViewTrigger.value = true
            intent.putExtra(EXTRA_FROM_NOTIFICATION_SIGN_IN, false)
        }
    }

    override fun onStop() {
        AndroidAppForegroundTracker.markForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        handleIntent(intent)
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
                        Toast.makeText(this@MainActivity, i18n("再按一次退出應用"), Toast.LENGTH_SHORT).show()
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
            @SuppressLint("RememberReturnType")
            val fontRepository = remember {
                DefaultFontRepository(
                    settingsStore = settingsStore,
                    appSettingsRepository = appSettingsRepository,
                    novelReaderSettingsRepository = novelReaderSettingsRepository,
                    platform = AndroidFontPlatform(context),
                )
            }

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
            val userSpaceRepository = remember { UserSpaceRepositoryImpl(cookieStore, yamiboClient, diskCacheFactory) }
            val blogRepository = remember { BlogRepositoryImpl(cookieStore, yamiboClient, diskCacheFactory) }
            val chineseConversionRepository = remember { createChineseConversionRepository() }
            val tagRepository = remember { AndroidTagRepository(cookieStore, yamiboClient, diskCacheFactory) }
            val favoriteRepository = remember { AndroidLocalFavoriteRepository(dbFactory) }
            val detailNoteRepository = remember { AndroidDetailNoteRepository(dbFactory) }
            val bookMarkRepository = remember { AndroidLocalBookMarkRepository(dbFactory) }
            val chapterStateRepository = remember { AndroidLocalChapterStateRepository(dbFactory) }
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
            val favoriteUpdateRepository = remember {
                FavoriteUpdateRepositoryImpl(
                    db = favoriteSyncDatabase,
                    localFavoriteRepository = favoriteRepository,
                    threadRepository = threadRepository,
                    tagRepository = tagRepository,
                )
            }
            val backgroundTaskRepository = remember { AndroidBackgroundTaskRepository(context) }
            @SuppressLint("RememberReturnType")
            val favoriteSyncRunner = remember { FavoriteSyncRunner(favoriteSyncRepository, backgroundTaskRepository) }
            val favoriteUpdateScheduler = remember { AndroidFavoriteUpdateScheduler(context) }
            @SuppressLint("RememberReturnType")
            val favoriteUpdateRunner = remember { FavoriteUpdateRunner(favoriteUpdateRepository, favoriteUpdateScheduler) }
            val backupStorageProvider = remember { AndroidBackupStorageProvider(context, appSettingsRepository) }
            val backupRepository = remember {
                BackupRepositoryImpl(
                    db = favoriteSyncDatabase,
                    settingsStore = settingsStore,
                    settingsRegistries = listOf(appSettingsRepository, novelReaderSettingsRepository, mangaReaderSettingsRepository),
                    storageProvider = backupStorageProvider,
                    appVersionCode = AppVersion.VersionCode.toInt(),
                )
            }
            val backupScheduler = remember { AndroidBackupScheduler(context) }
            val signReminderScheduler = remember { AndroidSignReminderScheduler(context) }
            LaunchedEffect(backupRepository) {
                diskCacheFactory.backupStorageUsageProvider = { backupRepository.getBackupStorageBytes() }
            }
            val backgroundAccessRepository = remember { AndroidBackgroundAccessRepository(context) }
            val novelCacheRepository = remember { AndroidNovelThreadCacheRepository(diskCacheFactory) }
            val inAppLinkNavigationRepository = remember {
                DefaultInAppLinkNavigationRepository(threadRepository, novelCacheRepository)
            }
            val readHistoryRepository = remember { AndroidReadHistoryRepository(dbFactory) }
            val signRepository = remember {
                AndroidSignRepository(
                    dbFactory = dbFactory,
                    authRepository = authRepository,
                    appSettingsRepository = appSettingsRepository,
                    yamiboClient = yamiboClient,
                )
            }
            val themeRepository = remember { AndroidThemeRepository() }
            val appUpdateRepository = remember {
                DefaultAppUpdateRepository(
                    appSettingsRepository = appSettingsRepository,
                    platform = AndroidAppUpdatePlatform(context),
                )
            }

            /** Provide Repositories */
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalAuthRepository provides authRepository,
                LocalAppUpdateRepository provides appUpdateRepository,
                LocalForumRepository provides forumRepository,
                LocalThreadRepository provides threadRepository,
                LocalInAppLinkNavigationRepository provides inAppLinkNavigationRepository,
                LocalUserSpaceRepository provides userSpaceRepository,
                LocalBlogRepository provides blogRepository,
                LocalBackupRepository provides backupRepository,
                LocalBackupScheduler provides backupScheduler,
                LocalChineseConversionRepository provides chineseConversionRepository,
                LocalDetailNoteRepository provides detailNoteRepository,
                LocalBookMarkRepository provides bookMarkRepository,
                LocalChapterStateRepository provides chapterStateRepository,
                LocalFavoriteRepository provides favoriteRepository,
                LocalRemoteFavoriteRepository provides remoteFavoriteRepository,
                LocalFavoriteSyncRepository provides favoriteSyncRepository,
                LocalFavoriteSyncRunner provides favoriteSyncRunner,
                LocalFavoriteUpdateRepository provides favoriteUpdateRepository,
                LocalFavoriteUpdateRunner provides favoriteUpdateRunner,
                LocalFontRepository provides fontRepository,
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
                LocalSignReminderScheduler provides signReminderScheduler,
            ) {
                val favoriteUpdateInterval = appSettingsRepository.favoriteUpdateInterval.state()
                val backupInterval = appSettingsRepository.backupInterval.state()
                val signReminderFrequency = appSettingsRepository.signInReminderFrequency.state()
                
                LaunchedEffect(Unit) {
                    if (appSettingsRepository.clearCacheOnAppLaunch.getValue()) {
                        delay(1_200)
                        diskCacheFactory.clearAllCache()
                    }
                }
                LaunchedEffect(favoriteUpdateInterval) {
                    delay(1_200)
                    favoriteUpdateRunner.schedulePeriodicUpdate(favoriteUpdateInterval)
                }
                LaunchedEffect(backupInterval) {
                    delay(1_200)
                    backupScheduler.schedule(backupInterval)
                }
                LaunchedEffect(signReminderFrequency) {
                    delay(1_200)
                    signReminderScheduler.schedule(signReminderFrequency)
                }
                LaunchedEffect(Unit) {
                    delay(1_200)
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

    companion object {
        const val EXTRA_FROM_NOTIFICATION_SIGN_IN = "extra_from_notification_sign_in"
    }
}
