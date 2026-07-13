package me.thenano.yamibo.yamibo_app.repository.settings.core

import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Registry base class for declarative settings container.
 * Manages the prefix binding and provides DSL for creating settings delegates.
 */
abstract class SettingsRegistry(
    @PublishedApi internal val store: SettingsStore,
    @PublishedApi internal val prefix: String,
) {
    private val exportableSettings = mutableListOf<SettingItem<*>>()

    val exportableSettingItems: List<SettingItem<*>>
        get() = exportableSettings.toList()

    private fun register(setting: SettingItem<*>) {
        if (exportableSettings.none { it.storageKey == setting.storageKey }) {
            exportableSettings += setting
        }
    }

    /** Helper class for delegation */
    class SettingDelegateProvider<T, out S : SettingItem<T>>(
        private val factory: (String) -> S
    ) {
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, S> {
            val setting = factory(property.name.lowercase())
            (thisRef as? SettingsRegistry)?.register(setting)
            return ReadOnlyProperty { _, _ -> setting }
        }
    }

    protected fun intSetting(
        name: String,
        default: Int,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        interval: Int = 1,
        description: String = ""
    ) = SettingDelegateProvider { propertyName ->
        val key = "$prefix.$propertyName"
        IntSetting(name, description, default, key, store, min, max, interval)
    }

    protected fun floatSetting(
        name: String,
        default: Float,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        interval: Float = 0f,
        description: String = ""
    ) = SettingDelegateProvider { propertyName ->
        val key = "$prefix.$propertyName"
        FloatSetting(name, description, default, key, store, min, max, interval)
    }

    protected fun boolSetting(
        name: String,
        default: Boolean,
        description: String = ""
    ) = SettingDelegateProvider { propertyName ->
        val key = "$prefix.$propertyName"
        BoolSetting(name, description, default, key, store)
    }

    protected fun stringSetting(
        name: String,
        default: String,
        allowedValues: List<String> = emptyList(),
        description: String = ""
    ) = SettingDelegateProvider { propertyName ->
        val key = "$prefix.$propertyName"
        StringSetting(name, description, default, key, store, allowedValues)
    }

    protected inline fun <reified T : Enum<T>> enumSetting(
        name: String,
        default: T,
        description: String = ""
    ) = SettingDelegateProvider { propertyName ->
        val key = "$prefix.$propertyName"
        EnumSetting(name, description, default, key, store, enumValues<T>())
    }
}
