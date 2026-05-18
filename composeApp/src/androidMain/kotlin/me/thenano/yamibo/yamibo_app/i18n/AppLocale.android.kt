package me.thenano.yamibo.yamibo_app.i18n

import android.annotation.SuppressLint
import android.os.Build
import android.os.LocaleList
import me.thenano.yamibo.yamibo_app.repository.settings.AppLanguage
import java.util.Locale


actual fun applyAppLocale(language: AppLanguage) {
    val locale = Locale.forLanguageTag(language.languageTag)
    Locale.setDefault(locale)

    @SuppressLint("ObsoleteSdkInt")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        LocaleList.setDefault(LocaleList(locale))
    }
}
