package me.thenano.yamibo.yamibo_app.repository.chineseconversion

/** Basic OpenCC-compatible conversion direction. */
enum class ChineseConversionMode(val id: String) {
    Simplified("t2s"),
    Traditional("s2t"),
}
