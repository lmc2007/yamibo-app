package me.thenano.yamibo.yamibo_app.repository.settings.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

abstract class SettingItem<T>(
    val name: String,
    val description: String,
    val default: T,
    protected val key: String,
    protected val store: SettingsStore
) {
    protected val flow: MutableStateFlow<T> by lazy { MutableStateFlow(loadFromStore()) }

    /** Observable StateFlow for Compose collectAsState(). */
    val stateFlow: StateFlow<T> by lazy { flow.asStateFlow() }

    abstract fun loadFromStore(): T
    abstract fun saveToStore(newValue: T)

    /** Get current value (snapshot). */
    fun getValue(): T = flow.value

    /** Update value, returns false if validation failed. */
    open fun setValue(newValue: T): Boolean {
        if (!validate(newValue)) return false
        saveToStore(newValue)
        flow.value = newValue
        return true
    }

    protected open fun validate(value: T): Boolean = true

    /** Reset to default */
    @Suppress("unused")
    fun reset() {
        setValue(default)
    }
}
