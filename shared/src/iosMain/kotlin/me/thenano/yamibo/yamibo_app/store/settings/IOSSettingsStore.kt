package me.thenano.yamibo.yamibo_app.store.settings

import platform.Foundation.NSUserDefaults

class IOSSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getInt(key: String, defaultValue: Int): Int {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.integerForKey(key).toInt()
    }

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.floatForKey(key)
    }

    override fun putFloat(key: String, value: Float) {
        defaults.setFloat(value, forKey = key)
    }

    override fun getString(key: String, defaultValue: String): String {
        return defaults.stringForKey(key) ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.boolForKey(key)
    }

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }

    override fun hasKey(key: String): Boolean {
        return defaults.objectForKey(key) != null
    }
}
