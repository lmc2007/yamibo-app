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

class CommentPostFactory(
    override val fetcher: FetchFactory
) : PostFactory(fetcher) {

    /**
     * Comment on a post in a thread.
     *
     * Performs a POST to `forum.php?mod=post&action=reply&comment=yes&...` with the given form
     * data. The server responds with an XML body containing a CDATA section with a success or error
     * message inside `#messagetext`.
     *
     * Success is determined by the presence of `succeedhandle` in the response body. The returned
     * message is extracted from the `#messagetext p` element.
     *
     * @param formHash The formhash token from the current session.
     * @param threadId The thread ID that contains the post.
     * @param postId The post ID to reply to.
     * @param message The reply message content.
     * @return [FetchResult.Success] containing the response message, or a [FetchResult.Failure] if
     * the request fails.
     */
    suspend fun commentPost(
        formHash: FormHash,
        threadId: ThreadId,
        postId: PostId,
        message: String
    ): FetchResult<String> {
        val url = YamiboRoute.PostComment(threadId = threadId, postId = postId).build()
        return try {
            val response =
                fetcher.perform(HttpMethod.Post, url) {
                    contentType(ContentType.Application.FormUrlEncoded.withCharset(Charsets.UTF_8))
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("formhash", formHash.value)
                                append("handlekey", "")
                                append("message", message)
                            }
                        )
                    )
                }

            val body = response.bodyAsText()
            val parsedMessage = PostResponseUtils.parseMessageText(body) ?: body

            if (response.status.isSuccess() && PostResponseUtils.isSuccess(body)) {
                FetchResult.Success(
                    value = parsedMessage,
                    statusCode = response.status.value,
                    url = url
                )
            } else {
                FetchResult.Failure.HttpError(
                    statusCode = response.status.value,
                    url = url,
                    bodyPreview = parsedMessage
                )
            }
        } catch (e: HttpRequestTimeoutException) {
            FetchResult.Failure.Timeout(url, e)
        } catch (e: Exception) {
            FetchResult.Failure.NetworkError(url, e)
        }
    }
}
