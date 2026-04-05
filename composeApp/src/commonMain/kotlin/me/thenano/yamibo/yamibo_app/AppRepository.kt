package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.compositionLocalOf
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.ForumRepository
import me.thenano.yamibo.yamibo_app.repository.NovelThreadCacheRepository
import me.thenano.yamibo.yamibo_app.repository.ReadHistoryRepository
import me.thenano.yamibo.yamibo_app.repository.TagRepository
import me.thenano.yamibo.yamibo_app.repository.ThemeRepository
import me.thenano.yamibo.yamibo_app.repository.ThreadRepository
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.MangaReaderSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.settings.NovelReaderSettingsRepository

val LocalAuthRepository =
    compositionLocalOf<AuthRepository> { error("LocalAuthRepository not provided") }

val LocalForumRepository =
    compositionLocalOf<ForumRepository> { error("LocalForumRepository not provided") }

val LocalThreadRepository =
    compositionLocalOf<ThreadRepository> { error("LocalThreadRepository not provided") }

val LocalThemeRepository =
    compositionLocalOf<ThemeRepository> { error("LocalThemeRepository not provided") }

val LocalNovelThreadCacheRepository =
    compositionLocalOf<NovelThreadCacheRepository> { error("LocalNovelThreadCacheRepository not provided") }

val LocalReadHistoryRepository =
    compositionLocalOf<ReadHistoryRepository> { error("LocalReadHistoryRepository not provided") }

val LocalTagRepository =
    compositionLocalOf<TagRepository> { error("LocalTagRepository not provided") }

val LocalAppSettingsRepository =
    compositionLocalOf<AppSettingsRepository> { error("LocalAppSettingsRepository not provided") }

val LocalNovelReaderSettingsRepository =
    compositionLocalOf<NovelReaderSettingsRepository> { error("LocalNovelReaderSettingsRepository not provided") }

val LocalMangaReaderSettingsRepository =
    compositionLocalOf<MangaReaderSettingsRepository> { error("LocalMangaReaderSettingsRepository not provided") }
