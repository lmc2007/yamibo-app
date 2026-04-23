package me.thenano.yamibo.yamibo_app.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.precision
import io.github.littlesurvival.YamiboRoute
import me.thenano.yamibo.yamibo_app.LocalAuthRepository

@Composable
fun rememberImageRequest(url: String, retryKey: Int = 0, enableCrossfade: Boolean = true): ImageRequest {
    val context = LocalPlatformContext.current
    val authRepo = LocalAuthRepository.current
    val fullUrl =
        if (url.startsWith("http")) url
        else "${YamiboRoute.Domain.build()}${url.removePrefix("/")}"

    val cookie = authRepo.cookieStore.load() ?: ""
    val isYamiboDomain = fullUrl.contains("yamibo.com")

    return remember(fullUrl, cookie, retryKey, enableCrossfade) {
        val builder = ImageRequest.Builder(context)
            .data(fullUrl)
            .memoryCacheKey(fullUrl)
            .diskCacheKey(fullUrl)
            .precision(Precision.INEXACT)
            .httpHeaders(
                NetworkHeaders.Builder().apply {
                    if (isYamiboDomain) {
                        add("Cookie", cookie)
                        add("Referer", "https://bbs.yamibo.com/")
                    }
                }.build()
            )
            .crossfade(enableCrossfade)

        if (retryKey > 0) {
            // Bypass memory/disk cache if the user explicitly clicked "Retry"
            builder.memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
        }
        builder.build()
    }
}
