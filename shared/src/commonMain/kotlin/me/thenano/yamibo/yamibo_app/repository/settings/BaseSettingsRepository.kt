package me.thenano.yamibo.yamibo_app.repository.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import me.thenano.yamibo.yamibo_app.store.settings.SettingsMigration
import me.thenano.yamibo.yamibo_app.store.settings.SettingsSerializer
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

/**
 * Base repository for typed, persistent settings backed by [SettingsStore].
 *
 * Subclasses only need to declare:
 * - A `@Serializable` data class `T` with default values
 * - The `serializer`, `prefix`, and `default`
 * - Optional [migrations] for key renames/deletions across versions
 *
 * Key names are automatically derived from the data class field paths.
 */
open class BaseSettingsRepository<T : Any>(
    store: SettingsStore,
    serializer: KSerializer<T>,
    prefix: String,
    default: T,
    migrations: List<SettingsMigration> = emptyList()
) {
    private val engine = SettingsSerializer(store, serializer, prefix, default, migrations)

    private val _settings: MutableStateFlow<T>

    val settings: StateFlow<T>

    init {
        engine.runMigrations()
        _settings = MutableStateFlow(engine.load())
        settings = _settings.asStateFlow()
    }

    /** Update settings using a mutator lambda and persist to store. */
    fun update(mutator: (T) -> T) {
        val next = mutator(_settings.value)
        engine.save(next)
        _settings.value = next
    }

    /** Reset all settings to their default values. */
    fun reset() {
        engine.reset()
        _settings.value = engine.load()
    }

    /** Reset a single field (e.g. "theme.mode") to its default. */
    fun resetField(fieldPath: String) {
        engine.resetField(fieldPath)
        _settings.value = engine.load()
    }

    /** Get all stored key paths for debugging. */
    fun allKeys(): List<String> = engine.allKeys()
}
