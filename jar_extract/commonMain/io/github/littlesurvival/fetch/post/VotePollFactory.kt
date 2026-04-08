package io.github.littlesurvival.fetch.post

import io.github.littlesurvival.YamiboRoute
import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.ForumId
import io.github.littlesurvival.dto.value.PollOptionId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.fetch.FetchFactory
import io.github.littlesurvival.fetch.PostFactory
import io.github.littlesurvival.fetch.post.util.PostResponseUtils
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets

class VotePollFactory(
    override val fetcher: FetchFactory
) : PostFactory(fetcher) {

    /**
     * Submit a vote for a poll.
     *
     * Performs a POST request to `forum.php?mod=misc&action=votepoll&fid=...`.
     * The form data contains the `formhash` and an array of `pollanswers[]`.
     *
     * @param formHash The formhash token from the current session.
     * @param forumId The forum ID where the thread belongs.
     * @param threadId The thread ID containing the poll.
     * @param options A list of options the user selected.
     * @return [FetchResult.Success] containing the response string if successful, or [FetchResult.Failure] otherwise.
     */
    suspend fun votePoll(
        formHash: FormHash,
        forumId: ForumId,
        threadId: ThreadId,
        options: List<PollOptionId>
    ): FetchResult<String> {
        val url = YamiboRoute.VotePoll(forumId, threadId).build()
        return try {
            val response =
                fetcher.perform(HttpMethod.Post, url) {
                    contentType(ContentType.Application.FormUrlEncoded.withCharset(Charsets.UTF_8))
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("formhash", formHash.value)
                                options.forEach { option ->
                                    append("pollanswers[]", option.value.toString())
                                }
                            }
                        )
                    )
                }

            val body = response.bodyAsText()
            val message = PostResponseUtils.parseMessageText(body) ?: body
            val successMessage = "投票成功, 感谢您的参与"

            if (response.status.isSuccess() && PostResponseUtils.isVoteSuccess(body)) {
                FetchResult.Success(value = successMessage, statusCode = response.status.value, url = url)
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