package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.navigation.ComposableNavigator
import me.thenano.yamibo.yamibo_app.navigation.LocalNavigator
import me.thenano.yamibo.yamibo_app.repository.IOSAuthRepository
import me.thenano.yamibo.yamibo_app.repository.IOSForumRepository
import me.thenano.yamibo.yamibo_app.repository.IOSThemeRepository
import me.thenano.yamibo.yamibo_app.repository.IOSThreadRepository
import me.thenano.yamibo.yamibo_app.repository.IOSNovelThreadCacheRepository
import me.thenano.yamibo.yamibo_app.repository.IOSReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.IOSTagRepository
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.store.IOSCookieStore
import me.thenano.yamibo.yamibo_app.store.IOSUserStore
import me.thenano.yamibo.yamibo_app.store.settings.IOSSettingsStore
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository

fun MainViewController() = ComposeUIViewController {
    /** Navigator Logic */
    val navigator = remember { ComposableNavigator() }

    /** Store Logic */
    val cookieStore = remember { IOSCookieStore() }
    val userStore = remember { IOSUserStore() }

    /** Repository Logic */
    val yamiboClient = remember { YamiboClient() }
    val authRepository = remember { IOSAuthRepository(cookieStore, userStore, yamiboClient) }
    val forumRepository = remember { IOSForumRepository(cookieStore, yamiboClient) }
    val threadRepository = remember { IOSThreadRepository(cookieStore, yamiboClient) }
    val novelCacheRepository = remember { IOSNovelThreadCacheRepository() }
    val dbFactory = remember { DatabaseFactory() }
    val readHistoryRepository = remember { IOSReadHistoryRepository(dbFactory) }
    val themeRepository = remember { IOSThemeRepository() }
    val tagRepository = remember { IOSTagRepository(cookieStore, yamiboClient) }
    val settingsStore = remember { IOSSettingsStore() }
    val appSettingsRepository = remember { AppSettingsRepository(settingsStore) }
    val novelReaderSettingsRepository = remember { NovelReaderSettingsRepository(settingsStore) }
    val mangaReaderSettingsRepository = remember { MangaReaderSettingsRepository(settingsStore) }

    /** Provide Repositories */
    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAuthRepository provides authRepository,
        LocalForumRepository provides forumRepository,
        LocalThreadRepository provides threadRepository,
        LocalNovelThreadCacheRepository provides novelCacheRepository,
        LocalReadHistoryRepository provides readHistoryRepository,
        LocalThemeRepository provides themeRepository,
        LocalTagRepository provides tagRepository,
        LocalAppSettingsRepository provides appSettingsRepository,
        LocalNovelReaderSettingsRepository provides novelReaderSettingsRepository,
        LocalMangaReaderSettingsRepository provides mangaReaderSettingsRepository,
    ) { App() }
}
