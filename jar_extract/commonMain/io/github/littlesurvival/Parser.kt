package io.github.littlesurvival

import io.github.littlesurvival.core.ParseResult

interface Parser<T> {
    suspend fun parse(html: String): ParseResult<T>
}