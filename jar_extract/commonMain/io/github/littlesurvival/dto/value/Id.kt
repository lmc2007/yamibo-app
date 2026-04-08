package io.github.littlesurvival.dto.value

import kotlin.jvm.JvmInline

interface Id
/** Type-safe forum id (fid). */
@JvmInline value class ForumId(val value: Int) : Id

/** Type-safe thread id (tid). */
@JvmInline value class ThreadId(val value: Int) : Id

/** Type-safe post id (pid). */
@JvmInline value class PostId(val value: Int) : Id

/** Type-safe user id (uid). */
@JvmInline value class UserId(val value: Int) : Id

/** Type-safe search id(sid) */
@JvmInline value class SearchId(val value: Int) : Id

/**
 * Type-safe favorite id (fvid)
 */
@JvmInline value class FavoriteId(val value: Int) : Id

/**
 * Type-safe tag id
 */
@JvmInline value class TagId(val value: Int) : Id