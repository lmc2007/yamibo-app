package io.github.littlesurvival

import io.github.littlesurvival.core.FetchResult
import io.github.littlesurvival.core.ParseResult
import io.github.littlesurvival.core.YamiboResult

interface Fetcher<T> {
    suspend fun getResult(url: String): FetchResult<T>
    fun setCookies(cookie: String)
    fun clearCookies()

}