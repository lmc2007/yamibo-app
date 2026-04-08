package io.github.littlesurvival.fetch

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*

internal actual fun createPlatformHttpClient(): HttpClient =
        HttpClient(Darwin) {
            install(HttpTimeout)
            engine { configureRequest { setAllowsCellularAccess(true) } }
        }

internal actual fun createPlatformHttpClientNoRedirect(): HttpClient =
        HttpClient(Darwin) {
            install(HttpTimeout)
            followRedirects = false
            engine { configureRequest { setAllowsCellularAccess(true) } }
        }
