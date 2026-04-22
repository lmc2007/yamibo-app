package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.compositionLocalOf
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository
import me.thenano.yamibo.yamibo_app.repository.ForumRepository
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository as LocalFavoriteRepositoryType
import me.thenano.yamibo.yamibo_app.repository.NovelPrePostCommentsCacheRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.SignRepository
import me.thenano.yamibo.yamibo_app.repository.TagRepository
import me.thenano.yamibo.yamibo_app.repository.ThemeRepository
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository
import me.thenano.yamibo.yamibo_app.profile.settings.access.BackgroundAccessRepository
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncRunner

val LocalAuthRepository =
    compositionLocalOf<AuthRepository> { error("LocalAuthRepository not provided") }

val LocalForumRepository =
    compositionLocalOf<ForumRepository> { error("LocalForumRepository not provided") }

val LocalThreadRepository =
    compositionLocalOf<ThreadRepository> { error("LocalThreadRepository not provided") }

val LocalFavoriteRepository =
    compositionLocalOf<LocalFavoriteRepositoryType> { error("LocalFavoriteRepository not provided") }

val LocalRemoteFavoriteRepository =
    compositionLocalOf<FavoriteRepository> { error("LocalRemoteFavoriteRepository not provided") }

val LocalFavoriteSyncRepository =
    compositionLocalOf<FavoriteSyncRepository> { error("LocalFavoriteSyncRepository not provided") }

val LocalFavoriteSyncRunner =
    compositionLocalOf<FavoriteSyncRunner> { error("LocalFavoriteSyncRunner not provided") }

val LocalBackgroundAccessRepository =
    compositionLocalOf<BackgroundAccessRepository> { error("LocalBackgroundAccessRepository not provided") }

val LocalThemeRepository =
    compositionLocalOf<ThemeRepository> { error("LocalThemeRepository not provided") }

val LocalNovelThreadCacheRepository =
    compositionLocalOf<NovelPrePostCommentsCacheRepository> { error("LocalNovelThreadCacheRepository not provided") }

val LocalReadHistoryRepository =
    compositionLocalOf<ReadHistoryRepository> { error("LocalReadHistoryRepository not provided") }

val LocalSignRepository =
    compositionLocalOf<SignRepository> { error("LocalSignRepository not provided") }

val LocalTagRepository =
    compositionLocalOf<TagRepository> { error("LocalTagRepository not provided") }

val LocalAppSettingsRepository =
    compositionLocalOf<AppSettingsRepository> { error("LocalAppSettingsRepository not provided") }

val LocalNovelReaderSettingsRepository =
    compositionLocalOf<NovelReaderSettingsRepository> { error("LocalNovelReaderSettingsRepository not provided") }

val LocalMangaReaderSettingsRepository =
    compositionLocalOf<MangaReaderSettingsRepository> { error("LocalMangaReaderSettingsRepository not provided") }

val LocalDiskCacheFactory =
    compositionLocalOf<me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory> { error("LocalDiskCacheFactory not provided") }
