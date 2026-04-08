package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.PostId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.PostFactory
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.Charsets

class RateFactory(override val fetcher: FetchFactory) : PostFactory(fetcher) {

    /**
     * Rate a post in a thread.
     *
     * Performs a POST to `forum.php?mod=misc&action=rate&ratesubmit=yes&...` with the given form
     * data. The server responds with an XML body containing a CDATA section with a success or error
     * message inside `#messagetext`.
     *
     * Success is determined by the presence of `succeedhandle` in the response body. The returned
     * message is extracted from the `#messagetext p` element.
     *
     * @param formHash The formhash token from the current session.
     * @param threadId The thread ID that contains the post.
     * @param postId The post ID to rate.
     * @param score The score to give (e.g. 1, 2, 5, 10).
     * @param reason Optional reason text for the rating.
     * @return [FetchResult.Success] containing the response body, or a [FetchResult.Failure] if the
     * request fails.
     */
    suspend fun addRate(
        formHash: FormHash,
        threadId: ThreadId,
        postId: PostId,
        score: Int,
        reason: String = ""
    ): FetchResult<String> {
        val url = YamiboRoute.Rate.build()
        return try {
            val response =
                fetcher.perform(HttpMethod.Post, url) {
                    contentType(ContentType.Application.FormUrlEncoded.withCharset(Charsets.UTF_8))
                    setBody(FormDataContent(Parameters.build {
                        append("formhash", formHash.value)
                        append("tid", threadId.value.toString())
                        append("pid", postId.value.toString())
                        append("referer", "")
                        append("handlekey", "rate")
                        append("score1", score.toString())
                        append("reason", reason)
                    }))
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
}
