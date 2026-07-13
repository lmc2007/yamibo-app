package me.thenano.yamibo.yamibo_app.i18n

internal fun i18n(source: String): String = i18n(source, *emptyArray<Any?>())

internal fun i18n(source: String, vararg args: Any?): String {
    var index = 0
    return Regex("\\{\\}").replace(source) {
        val value = args.getOrNull(index)
        index += 1
        value?.toString() ?: "{}"
    }
}
