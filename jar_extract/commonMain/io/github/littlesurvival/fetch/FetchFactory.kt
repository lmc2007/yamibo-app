package io.github.littlesurvival.fetch

import io.github.littlesurvival.Fetcher
import io.github.littlesurvival.core.FetchResult
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal expect fun createPlatformHttpClient(): HttpClient

internal expect fun createPlatformHttpClientNoRedirect(): HttpClient

class FetchFactory(
    var device: Device,
    var timeoutMillis: Long,
) : Fetcher<String> {

    enum class Device(val userAgent: String) {
        MOBILE("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"),
        DESKTOP("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
    }

    /** Raw cookie string to send with every request. e.g. "key=value; key2=value2" */
    private var cookieString: String? = null

    private val client = createPlatformHttpClient()
    private val noRedirectClient = createPlatformHttpClientNoRedirect()

    /**
     * Set cookie string for all requests.
     * @param cookie The raw cookie string in format "key=value; key2=value2"
     */
    override fun setCookies(cookie: String) {
        cookieString = cookie.replace("\n", "").trim()
    }

    /** Clear cookies. */
    override fun clearCookies() {
        cookieString = null
    }


    /**
     * @param noRedirect perform a request without following redirects.
     *
     * Useful for POST requests where the server responds with 302 and we need to capture the
     * Location header.
     */
    suspend fun perform(
        method: HttpMethod,
        url: String,
        noRedirect: Boolean = false,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val useClient = if (noRedirect) noRedirectClient else client
        return useClient.request(url) {
            this.method = method
            headers[HttpHeaders.UserAgent] = device.userAgent
            cookieString?.let { headers[HttpHeaders.Cookie] = it }
            timeout {
                requestTimeoutMillis = timeoutMillis
                connectTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
            block()
        }
    }

    /**
     * Fetcher return the HTML content of the page as FetchResult.
     *
     * @param url The url to fetch.
     * @return The HTML content of the page as FetchResult.
     *
     * FetchResult.Success: The HTML content of the page. FetchResult.Failure: The error occurred
     * during the fetch.
     */
    override suspend fun getResult(url: String): FetchResult<String> {
        return try {
            val response = perform(HttpMethod.Get, url)
            val text = response.bodyAsText()

            if (response.status.isSuccess()) {
                FetchResult.Success(value = text, statusCode = response.status.value, url = url)
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = text
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }
}
