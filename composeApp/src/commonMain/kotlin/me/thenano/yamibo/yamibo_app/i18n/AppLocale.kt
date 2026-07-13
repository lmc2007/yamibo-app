package me.thenano.yamibo.yamibo_app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import kotlinx.coroutines.runBlocking
import me.thenano.yamibo.yamibo_app.repository.settings.AppLanguage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

private const val AppStringCacheMaxSize = 1024

private data class AppStringCacheKey(
    val languageTag: String,
    val resourceKey: String,
    val args: List<String>,
)

private var currentAppLanguageTag: String = AppLanguage.TRADITIONAL_CHINESE.languageTag
private var appStringCache: Map<AppStringCacheKey, String> = emptyMap()

@Composable
fun AppLocaleProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val localeKey = remember(language) {
        applyAppLanguage(language)
    }

    key(localeKey) {
        content()
    }
}

expect fun applyAppLocale(language: AppLanguage)

private fun applyAppLanguage(language: AppLanguage): String {
    if (currentAppLanguageTag != language.languageTag) {
        currentAppLanguageTag = language.languageTag
        appStringCache = emptyMap()
    }
    applyAppLocale(language)
    return language.languageTag
}

fun appString(resource: StringResource, vararg formatArgs: Any): String {
    val args = formatArgs.map { it.toString() }
    val key = AppStringCacheKey(currentAppLanguageTag, resource.key, args)
    appStringCache[key]?.let { return it }

    val value = runBlocking {
        getString(resource, *formatArgs)
    }
    appStringCache = if (appStringCache.size >= AppStringCacheMaxSize) {
        mapOf(key to value)
    } else {
        appStringCache + (key to value)
    }
    return value
}
