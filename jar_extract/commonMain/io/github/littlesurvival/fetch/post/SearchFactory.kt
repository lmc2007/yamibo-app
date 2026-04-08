package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.page.SearchPage
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.PostFactory
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.utils.io.charsets.Charsets

class SearchFactory(
    override val fetcher: FetchFactory,
) : PostFactory(fetcher) {
    /**
     * Submit a search query and return the redirect location URL.
     *
     * Performs a POST to `search.php?mod=forum` with the given [formHash] and [query]. The server
     * responds with 302 and a `Location` header pointing to the search results page.
     *
     * @param formHash The formhash token from the current session.
     * @param query The search text.
     * @return [FetchResult.Success] containing a [kotlin.String] with the redirect location, or a
     * [FetchResult.Failure] if the request fails.
     */
    suspend fun getCacheLink(formHash: FormHash, query: String, fId: ForumId?): FetchResult<String> {
        val url = YamiboRoute.Search.SearchPhp(fId).build()
        return try {
            val response =
                fetcher.perform(method = HttpMethod.Post, url = url, noRedirect = true) {
                    contentType(ContentType.Application.FormUrlEncoded.withCharset(Charsets.UTF_8))
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("formhash", formHash.value)
                                append("srhfid", fId?.value?.toString() ?: "")
                                append("srchtxt", query)
                                append("searchsubmit", "yes")
                            }
                        )
                    )
                }

            if (response.status == HttpStatusCode.Found) {
                val location = response.headers[HttpHeaders.Location]
                if (location != null) {
                    FetchResult.Success(
                        value = YamiboRoute.Search.ByLocation(location).build(),
                        statusCode = response.status.value,
                        url = url
                    )
                } else {
                    FetchResult.Failure.HttpError(
                        statusCode = response.status.value,
                        url = url,
                        bodyPreview = response.bodyAsText()
                    )
                }
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = response.bodyAsText()
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }
}
