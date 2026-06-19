package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.compositionLocalOf
import me.thenano.yamibo.yamibo_app.core.cache.DiskCacheFactory
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.AppUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.BlogRepository
import me.thenano.yamibo.yamibo_app.repository.BackupRepository
import me.thenano.yamibo.yamibo_app.repository.ChineseConversionRepository
import me.thenano.yamibo.yamibo_app.repository.ContentCoverRepository
import me.thenano.yamibo.yamibo_app.repository.DetailNoteRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteSyncRepository
import me.thenano.yamibo.yamibo_app.repository.FavoriteUpdateRepository
import me.thenano.yamibo.yamibo_app.repository.FontRepository
import me.thenano.yamibo.yamibo_app.repository.ForumRepository
import me.thenano.yamibo.yamibo_app.repository.InAppLinkNavigationRepository
import me.thenano.yamibo.yamibo_app.repository.LocalBookMarkRepository as LocalBookMarkRepositoryType
import me.thenano.yamibo.yamibo_app.repository.LocalChapterStateRepository as LocalChapterStateRepositoryType
import me.thenano.yamibo.yamibo_app.repository.LocalFavoriteRepository as LocalFavoriteRepositoryType
import me.thenano.yamibo.yamibo_app.repository.NovelPrePostCommentsCacheRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.SignRepository
import me.thenano.yamibo.yamibo_app.repository.TagRepository
import me.thenano.yamibo.yamibo_app.repository.ThemeRepository
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository
import me.thenano.yamibo.yamibo_app.repository.UserSpaceRepository
import me.thenano.yamibo.yamibo_app.profile.settings.access.BackgroundAccessRepository
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.favorite.sync.FavoriteSyncRunner
import me.thenano.yamibo.yamibo_app.favorite.updates.FavoriteUpdateRunner
import me.thenano.yamibo.yamibo_app.profile.settings.backup.BackupScheduler
import me.thenano.yamibo.yamibo_app.profile.settings.sign.SignReminderScheduler

val LocalAuthRepository =
    compositionLocalOf<AuthRepository> { error("LocalAuthRepository not provided") }

val LocalAppUpdateRepository =
    compositionLocalOf<AppUpdateRepository> { error("LocalAppUpdateRepository not provided") }

val LocalForumRepository =
    compositionLocalOf<ForumRepository> { error("LocalForumRepository not provided") }

val LocalThreadRepository =
    compositionLocalOf<ThreadRepository> { error("LocalThreadRepository not provided") }

val LocalInAppLinkNavigationRepository =
    compositionLocalOf<InAppLinkNavigationRepository> { error("LocalInAppLinkNavigationRepository not provided") }

val LocalUserSpaceRepository =
    compositionLocalOf<UserSpaceRepository> { error("LocalUserSpaceRepository not provided") }

val LocalBlogRepository =
    compositionLocalOf<BlogRepository> { error("LocalBlogRepository not provided") }

val LocalBackupRepository =
    compositionLocalOf<BackupRepository> { error("LocalBackupRepository not provided") }

val LocalBackupScheduler =
    compositionLocalOf<BackupScheduler> { error("LocalBackupScheduler not provided") }

val LocalSignReminderScheduler =
    compositionLocalOf<SignReminderScheduler> { error("LocalSignReminderScheduler not provided") }

val LocalChineseConversionRepository =
    compositionLocalOf<ChineseConversionRepository> { error("LocalChineseConversionRepository not provided") }

val LocalContentCoverRepository =
    compositionLocalOf<ContentCoverRepository> { error("LocalContentCoverRepository not provided") }

val LocalDetailNoteRepository =
    compositionLocalOf<DetailNoteRepository> { error("LocalDetailNoteRepository not provided") }

val LocalBookMarkRepository =
    compositionLocalOf<LocalBookMarkRepositoryType> { error("LocalBookMarkRepository not provided") }

val LocalChapterStateRepository =
    compositionLocalOf<LocalChapterStateRepositoryType> { error("LocalChapterStateRepository not provided") }

val LocalFavoriteRepository =
    compositionLocalOf<LocalFavoriteRepositoryType> { error("LocalFavoriteRepository not provided") }

val LocalRemoteFavoriteRepository =
    compositionLocalOf<FavoriteRepository> { error("LocalRemoteFavoriteRepository not provided") }

val LocalFavoriteSyncRepository =
    compositionLocalOf<FavoriteSyncRepository> { error("LocalFavoriteSyncRepository not provided") }

val LocalFavoriteSyncRunner =
    compositionLocalOf<FavoriteSyncRunner> { error("LocalFavoriteSyncRunner not provided") }

val LocalFavoriteUpdateRepository =
    compositionLocalOf<FavoriteUpdateRepository> { error("LocalFavoriteUpdateRepository not provided") }

val LocalFavoriteUpdateRunner =
    compositionLocalOf<FavoriteUpdateRunner> { error("LocalFavoriteUpdateRunner not provided") }

val LocalFontRepository =
    compositionLocalOf<FontRepository> { error("LocalFontRepository not provided") }

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
    compositionLocalOf<DiskCacheFactory> { error("LocalDiskCacheFactory not provided") }
