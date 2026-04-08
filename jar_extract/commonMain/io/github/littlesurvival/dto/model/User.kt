package io.github.littlesurvival.dto.model

import io.github.littlesurvival.dto.value.UserId

/** Forum user information. */
data class User(
    /** User id (uid). */
    val uid: UserId,

    /** User display name. */
    val name: String,

    /** URL to the user's avatar image. */
    val avatarUrl: String? = null
)
