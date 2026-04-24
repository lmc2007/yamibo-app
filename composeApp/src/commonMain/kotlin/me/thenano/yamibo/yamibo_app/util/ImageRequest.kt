package me.thenano.yamibo.yamibo_app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import io.github.littlesurvival.YamiboRoute
import me.thenano.yamibo.yamibo_app.LocalAuthRepository

fun normalizeImageUrl(url: String): String {
    return if (url.startsWith("http")) {
        url
    } else {
        "${YamiboRoute.Domain.build()}${url.removePrefix("/")}"
    }
}

fun buildImageRequest(
    context: PlatformContext,
    url: String,
    cookie: String = "",
    retryKey: Int = 0,
    enableCrossfade: Boolean = true,
): ImageRequest {
    val fullUrl = normalizeImageUrl(url)
    val isYamiboDomain = fullUrl.contains("yamibo.com")

    return ImageRequest.Builder(context)
        .data(fullUrl)
        .memoryCacheKey(fullUrl)
        .diskCacheKey(fullUrl)
        .precision(Precision.INEXACT)
        .httpHeaders(
            NetworkHeaders.Builder().apply {
                if (isYamiboDomain) {
                    add("Cookie", cookie)
                    add("Referer", YamiboRoute.Domain.build())
                }
            }.build()
        )
        .crossfade(enableCrossfade)
        .apply {
            if (retryKey > 0) {
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
            }
        }
        .build()
}

@Composable
fun rememberImageRequest(url: String, retryKey: Int = 0, enableCrossfade: Boolean = true): ImageRequest {
    val context = LocalPlatformContext.current
    val authRepo = LocalAuthRepository.current
    val fullUrl = normalizeImageUrl(url)
    val cookie = authRepo.cookieStore.load().orEmpty()

    return remember(fullUrl, cookie, retryKey, enableCrossfade) {
        buildImageRequest(
            context = context,
            url = fullUrl,
            cookie = cookie,
            retryKey = retryKey,
            enableCrossfade = enableCrossfade,
        )
    }
}
