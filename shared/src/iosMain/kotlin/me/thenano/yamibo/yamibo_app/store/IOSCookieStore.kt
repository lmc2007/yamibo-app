package me.thenano.yamibo.yamibo_app.store

import me.thenano.yamibo.yamibo_app.store.auth.CookieStore
import platform.Foundation.NSUserDefaults

class IOSCookieStore : CookieStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun save(value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun load(): String? {
        return defaults.stringForKey(key)
    }

    override fun clear() {
        defaults.removeObjectForKey(key)
    }
}
