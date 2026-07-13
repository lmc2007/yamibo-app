package me.thenano.yamibo.yamibo_app.store.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("UseKtx")
class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("yamibo_app_settings", Context.MODE_PRIVATE)

    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    override fun getFloat(key: String, defaultValue: Float): Float = prefs.getFloat(key, defaultValue)
    override fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()

    override fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    override fun remove(key: String) = prefs.edit().remove(key).apply()
    override fun hasKey(key: String): Boolean = prefs.contains(key)
}
