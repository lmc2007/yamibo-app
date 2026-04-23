package me.thenano.yamibo.yamibo_app.store

import android.annotation.SuppressLint
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import me.thenano.yamibo.yamibo_app.store.auth.CookieStore

class AndroidCookieStore(
    context: Context
) : CookieStore {
    private val prefs = EncryptedSharedPreferences.create(
        context, prefName, MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    @SuppressLint("UseKtx")
    override fun save(value: String) {
        prefs.edit().putString(key, value).commit()
    }

    override fun load(): String? {
        return prefs.getString(key, null)
    }

    @SuppressLint("UseKtx")
    override fun clear() {
        prefs.edit().clear().commit()
    }
}
