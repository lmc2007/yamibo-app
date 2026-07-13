package me.thenano.yamibo.yamibo_app.factory

import io.ktor.client.*
import io.ktor.client.plugins.*

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object HttpClientFactory {
    fun create(defaultHeaders: DefaultRequest.DefaultRequestBuilder.() -> Unit = {}): HttpClient
}