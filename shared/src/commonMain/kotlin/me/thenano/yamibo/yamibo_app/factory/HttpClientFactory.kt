package me.thenano.yamibo.yamibo_app.factory

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest

expect object HttpClientFactory {
    fun create(defaultHeaders: DefaultRequest.DefaultRequestBuilder.() -> Unit = {}): HttpClient
}