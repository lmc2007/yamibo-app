package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import com.fleeksoft.ksoup.Ksoup
import io.github.littlesurvival.fetch.PostFactory
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

class FavoriteFactory(override val fetcher: FetchFactory) : PostFactory(fetcher) {

    /**
     * Add a thread to the user's favorites.
     *
     * Performs a POST to `home.php?mod=spacecp&ac=favorite&type=thread&id=...` with the given
     * [formHash] and [threadId]. The server responds with an XML body containing a CDATA section
     * with a success or error message inside `#messagetext`.
     *
     * Success is determined by the presence of `succeedhandle` in the response body. The returned
     * message is extracted from the `#messagetext p` element.
     *
     * @param formHash The formhash token from the current session.
     * @param threadId The thread ID to add to favorites.
     * @return [FetchResult.Success] containing the response body, or a [FetchResult.Failure] if the
     * request fails.
     */
    suspend fun addThread(formHash: FormHash, threadId: ThreadId): FetchResult<String> {
        val url = YamiboRoute.Favorite.AddThread(threadId).build()
        val referer = YamiboRoute.Thread(threadId).build()
        return try {
            val response =
                fetcher.perform(HttpMethod.Post, url) {
                    header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("favoritesubmit", "true")
                                append("referer", referer)
                                append("formhash", formHash.value)
                                append("description", "手机收藏")
                            }
                        )
                    )
                }

            val body = response.bodyAsText()
            val message = PostResponseUtils.parseMessageText(body) ?: body

            if (response.status.isSuccess() && PostResponseUtils.isSuccess(body)) {
                FetchResult.Success(value = message, statusCode = response.status.value, url = url)
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = message
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }

    /**
     * Add a forum to the user's favorites.
     *
     * Performs a GET to `home.php?mod=spacecp&ac=favorite&type=forum&id=...` with the given
     * [formHash] and [forumId]. The server responds with an HTML page containing the result
     * message inside a `.jump_c p` element.
     *
     * @param formHash The formhash token from the current session.
     * @param forumId The forum ID to add to favorites.
     * @return [FetchResult.Success] containing the response message, or a [FetchResult.Failure] if the
     * request fails.
     */
    suspend fun addForum(formHash: FormHash, forumId: ForumId): FetchResult<String> {
        val url = YamiboRoute.Favorite.AddForum(forumId, formHash).build()
        return try {
            val response = fetcher.perform(HttpMethod.Get, url)

            val body = response.bodyAsText()
            val doc = Ksoup.parse(body)
            // Extract the first message paragraph from jump_c
            val message = doc.selectFirst(".jump_c p")?.text()?.trim() ?: body

            if (response.status.isSuccess() && !PostResponseUtils.isIllegal(body)) {
                FetchResult.Success(value = message, statusCode = response.status.value, url = url)
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = message
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }
}
