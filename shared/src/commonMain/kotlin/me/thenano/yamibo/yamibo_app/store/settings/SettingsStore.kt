package me.thenano.yamibo.yamibo_app.store.settings

interface SettingsStore {
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)

    fun getFloat(key: String, defaultValue: Float): Float
    fun putFloat(key: String, value: Float)

    fun getString(key: String, defaultValue: String): String
    fun putString(key: String, value: String)

    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)

    fun remove(key: String)
    fun hasKey(key: String): Boolean
}
