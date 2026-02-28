package me.thenano.yamibo.yamibo_app.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import me.thenano.yamibo.yamibo_app.UserAgent
import me.thenano.yamibo.yamibo_app.factory.HttpClientFactory
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore

object AuthFetcher {
    private val client =
        HttpClientFactory.create {
            header(HttpHeaders.CacheControl, "max-age=0")
            header(HttpHeaders.UserAgent, UserAgent.WINDOWS)
        }

    suspend fun fetch(url: String, cookieStore: CookieStore): String? {
        return try {
            client
                .get {
                    url(url)
                    header(HttpHeaders.Cookie, cookieStore.load() ?: "")
                }
                .bodyAsText()
        } catch (e: Exception) {
            e.message
        }
    }
}
