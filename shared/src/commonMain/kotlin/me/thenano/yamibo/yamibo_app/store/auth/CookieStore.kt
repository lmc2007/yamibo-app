package me.thenano.yamibo.yamibo_app.store.auth


interface CookieStore {
    val prefName: String get() = "session_store"
    val key: String get() = "cookie"
    fun save(value: String)
    fun load(): String?
    fun clear()
}