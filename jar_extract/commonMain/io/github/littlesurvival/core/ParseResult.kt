package io.github.littlesurvival.core

sealed class ParseResult<out T> {

    data class Success<T>(val value: T) : ParseResult<T>()

    data class Failure(val reason: String, val exception: Throwable? = null) :
            ParseResult<Nothing>()

    /**
     * The HTML response is a login page, meaning the user is not logged in or their session has
     * expired.
     */
    data object NotLoggedIn : ParseResult<Nothing>()

    /** The website is currently under maintenance. */
    data object Maintenance : ParseResult<Nothing>()

    /** The user does not have permission to view this content. */
    data class NoPermission(val reason: String) : ParseResult<Nothing>()
}
