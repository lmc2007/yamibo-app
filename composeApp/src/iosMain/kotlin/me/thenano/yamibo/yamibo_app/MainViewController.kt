@file:Suppress("FunctionName", "unused")

package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.favorite.sync.IOSBackgroundTaskRepository
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.navigation.rememberRestorableNavigator
import me.thenano.yamibo.yamibo_app.profile.settings.access.IOSBackgroundAccessRepository
import me.thenano.yamibo.yamibo_app.repository.*
import me.thenano.yamibo.yamibo_app.repository.favorite.FavoriteSyncRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.store.IOSCookieStore
import me.thenano.yamibo.yamibo_app.store.IOSUserStore
import me.thenano.yamibo.yamibo_app.store.settings.IOSSettingsStore

fun MainViewController() = ComposeUIViewController {
    /** Navigator Logic */
    val navigator = rememberRestorableNavigator()

    /** Store Logic */
    val cookieStore = remember { IOSCookieStore() }
    val userStore = remember { IOSUserStore() }
    val settingsStore = remember { IOSSettingsStore() }
    val appSettingsRepository = remember { AppSettingsRepository(settingsStore) }
    val novelReaderSettingsRepository = remember { NovelReaderSettingsRepository(settingsStore) }
    val mangaReaderSettingsRepository = remember { MangaReaderSettingsRepository(settingsStore) }

    /** Repository Logic */
    val yamiboClient = remember { YamiboClient() }
    val authRepository = remember { IOSAuthRepository(cookieStore, userStore, yamiboClient) }
    
    val dbFactory = remember { DatabaseFactory() }
    val diskCacheFactory = remember { 
        val paths = platform.Foundation.NSSearchPathForDirectoriesInDomains(
            platform.Foundation.NSCachesDirectory, 
            platform.Foundation.NSUserDomainMask, 
            true
        )
        val cacheDir = paths.first() as String
        me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory(dbFactory, cacheDirPath = cacheDir) 
    }

    val forumRepository = remember { IOSForumRepository(cookieStore, yamiboClient, diskCacheFactory) }
    val threadRepository = remember { IOSThreadRepository(cookieStore, yamiboClient, diskCacheFactory) }
    val favoriteRepository = remember { IOSLocalFavoriteRepository(dbFactory) }
    val remoteFavoriteRepository = remember { IOSFavoriteRepository(cookieStore, yamiboClient) }
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
    val backgroundTaskRepository = remember { IOSBackgroundTaskRepository(favoriteSyncRepository) }
    val favoriteSyncRunner = remember { FavoriteSyncRunner(favoriteSyncRepository, backgroundTaskRepository) }
    val backgroundAccessRepository = remember { IOSBackgroundAccessRepository() }
    val novelCacheRepository = remember { IOSNovelThreadCacheRepository(diskCacheFactory) }
    val readHistoryRepository = remember { IOSReadHistoryRepository(dbFactory) }
    val signRepository = remember { IOSSignRepository(dbFactory, authRepository, appSettingsRepository) }
    val themeRepository = remember { IOSThemeRepository() }
    val tagRepository = remember { IOSTagRepository(cookieStore, yamiboClient, diskCacheFactory) }

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
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (appSettingsRepository.clearCacheOnAppLaunch.getValue()) {
                diskCacheFactory.clearAllCache()
            }
        }
        App()
    }
}
