package io.github.littlesurvival.core

sealed class FetchResult<out T> {

    data class Success<T>(
        val value: T,
        val statusCode: Int,
        val url: String
    ) : FetchResult<T>()

    sealed class Failure(open val url: String) : FetchResult<Nothing>() {

        /**
         * @param bodyPreview force to use response body if exists.
         */
        data class HttpError(
            val statusCode: Int,
            override val url: String,
            val bodyPreview: String?
        ) : Failure(url)

//        data class PostError(
//            val statusCode: Int,
//            override val url: String,
//            val message: String,
//        ) : Failure(url)

        data class NetworkError(
            override val url: String,
            val exception: Throwable
        ) : Failure(url)

        data class Timeout(
            override val url: String,
            val exception: Throwable
        ) : Failure(url)

        data class Unknown(
            override val url: String,
            val exception: Throwable
        ) : Failure(url)
    }
}