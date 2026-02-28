package me.thenano.yamibo.yamibo_app

import androidx.compose.runtime.compositionLocalOf
import me.thenano.yamibo.yamibo_app.repository.AuthRepository
import me.thenano.yamibo.yamibo_app.repository.ForumRepository
import me.thenano.yamibo.yamibo_app.repository.ThemeRepository

val LocalAuthRepository = compositionLocalOf<AuthRepository> {
    error("LocalAuthRepository not provided")
}

val LocalForumRepository = compositionLocalOf<ForumRepository> {
    error("LocalForumRepository not provided")
}

val LocalThemeRepository = compositionLocalOf<ThemeRepository> {
    error("LocalThemeRepository not provided")
}