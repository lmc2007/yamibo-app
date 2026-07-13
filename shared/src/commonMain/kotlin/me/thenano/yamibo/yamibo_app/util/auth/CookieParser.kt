package me.thenano.yamibo.yamibo_app.util.auth

fun parseCookieStringToMap(cookieString: String?): Map<String, String> {
    if (cookieString.isNullOrEmpty()) return emptyMap()
    return cookieString
        .split(";")
        .mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) return@mapNotNull null

            val key = part.substring(0, index).trim()
            val value = part.substring(index + 1).trim()

            if (key.isEmpty()) null else key to value
        }
        .toMap()
}

