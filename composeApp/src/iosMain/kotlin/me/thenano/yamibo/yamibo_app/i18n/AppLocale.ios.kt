package me.thenano.yamibo.yamibo_app.i18n

import me.thenano.yamibo.yamibo_app.repository.settings.AppLanguage
import platform.Foundation.NSUserDefaults

actual fun applyAppLocale(language: AppLanguage) {
    NSUserDefaults.standardUserDefaults.setObject(
        listOf(language.languageTag),
        forKey = "AppleLanguages",
    )
    NSUserDefaults.standardUserDefaults.synchronize()
}
