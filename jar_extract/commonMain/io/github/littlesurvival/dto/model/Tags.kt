package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.TagId

data class Tags(
    val value: List<TagValue> = emptyList()
)

data class TagValue(
    val id: TagId,
    val name: String,
)