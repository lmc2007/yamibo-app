package me.thenano.yamibo.yamibo_app.store.settings

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Flattens a @Serializable data class into a SettingsStore using qualified dot-separated keys.
 *
 * Key format: `{prefix}.{nestedField}.{leafField}` (all lowercase).
 *
 * Example:
 * ```
 * @Serializable data class Theme(val mode: String = "SYSTEM")
 * @Serializable data class AppSettings(val theme: Theme = Theme())
 *
 * // prefix = "appsettings"
 * // keys: "appsettings.theme.mode" = "SYSTEM"
 * ```
 */
class SettingsSerializer<T : Any>(
    private val store: SettingsStore,
    private val serializer: KSerializer<T>,
    private val prefix: String,
    private val default: T,
    private val migrations: List<SettingsMigration> = emptyList()
) {
    private val versionKey = "__settings_version__.$prefix"

    /** Cached default values (key → string). Built lazily from [default]. */
    private val defaultValues: Map<String, String> by lazy {
        val encoder = CapturingEncoder(prefix)
        encoder.encodeSerializableValue(serializer, default)
        encoder.captured
    }

    /** Run any pending migrations (oldest-first). */
    fun runMigrations() {
        if (migrations.isEmpty()) return
        val currentVersion = store.getInt(versionKey, 0)
        val pending = migrations.filter { it.version > currentVersion }.sortedBy { it.version }
        for (migration in pending) {
            for ((oldKey, newKey) in migration.renames) {
                if (store.hasKey(oldKey)) {
                    val value = store.getString(oldKey, "")
                    store.putString(newKey, value)
                    store.remove(oldKey)
                    println("[Settings] Migrated key: $oldKey → $newKey")
                }
            }
            for (key in migration.deletions) {
                if (store.hasKey(key)) {
                    store.remove(key)
                    println("[Settings] Deleted key: $key")
                }
            }
            store.putInt(versionKey, migration.version)
        }
    }

    /** Load from store, falling back to default values for missing keys. */
    fun load(): T {
        val decoder = SettingsStoreDecoder(store, prefix, defaultValues)
        return decoder.decodeSerializableValue(serializer)
    }

    /** Save all fields to store. */
    fun save(value: T) {
        val encoder = SettingsStoreEncoder(store, prefix)
        encoder.encodeSerializableValue(serializer, value)
    }

    /** Collect all leaf keys for this serializer. */
    fun allKeys(): List<String> {
        return defaultValues.keys.toList()
    }

    /** Reset all fields to their default values. */
    fun reset() {
        save(default)
    }

    /** Reset a single field path (e.g. "theme.mode") to its default value. */
    fun resetField(fieldPath: String) {
        val fullKey = "$prefix.$fieldPath"
        val defaultValue = defaultValues[fullKey]
        if (defaultValue != null) {
            store.putString(fullKey, defaultValue)
            println("[Settings] Reset key: $fullKey → $defaultValue")
        } else {
            println("[Settings] Reset failed: key '$fullKey' not found in defaults. Available: ${defaultValues.keys}")
        }
    }
}

// ─── Encoder: Data Class → SettingsStore ───

@OptIn(ExperimentalSerializationApi::class)
private class SettingsStoreEncoder(
    private val store: SettingsStore,
    private val prefix: String
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    private var currentKey = ""

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentKey = "$prefix.${descriptor.getElementName(index).lowercase()}"
        return true
    }

    override fun encodeInt(value: Int) = store.putInt(currentKey, value)
    override fun encodeFloat(value: Float) = store.putFloat(currentKey, value)
    override fun encodeString(value: String) = store.putString(currentKey, value)
    override fun encodeBoolean(value: Boolean) = store.putBoolean(currentKey, value)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return if (currentKey.isEmpty()) this
        else SettingsStoreEncoder(store, currentKey)
    }

    override fun endStructure(descriptor: SerialDescriptor) { /* no-op */ }
}

// ─── Decoder: SettingsStore → Data Class (with defaults fallback) ───

/**
 * Reads flat KV pairs from [SettingsStore] back into a @Serializable data class.
 * Uses [defaults] map as fallback for missing keys.
 *
 * Strategy: [decodeElementIndex] yields sequential indices so the generated
 * deserializer calls [decodeInt]/[decodeFloat]/[decodeString]/[decodeBoolean]
 * in order. Before each element is decoded, [decodeElementIndex] records the
 * current key so the subsequent decode*() call knows which key to read.
 */
@OptIn(ExperimentalSerializationApi::class)
private class SettingsStoreDecoder(
    private val store: SettingsStore,
    private val prefix: String,
    private val defaults: Map<String, String>
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    private var elementIndex = 0
    private var currentKey = ""
    private var currentDescriptor: SerialDescriptor? = null

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex >= descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        currentDescriptor = descriptor
        val idx = elementIndex
        currentKey = "$prefix.${descriptor.getElementName(idx).lowercase()}"
        elementIndex++
        return idx
    }

    override fun decodeNotNullMark(): Boolean = true

    override fun decodeInt(): Int {
        logIfMissing(currentKey)
        return store.getInt(currentKey, defaults[currentKey]?.toIntOrNull() ?: 0)
    }

    override fun decodeFloat(): Float {
        logIfMissing(currentKey)
        return store.getFloat(currentKey, defaults[currentKey]?.toFloatOrNull() ?: 0f)
    }

    override fun decodeString(): String {
        logIfMissing(currentKey)
        return store.getString(currentKey, defaults[currentKey] ?: "")
    }

    override fun decodeBoolean(): Boolean {
        logIfMissing(currentKey)
        return store.getBoolean(currentKey, defaults[currentKey]?.toBooleanStrictOrNull() ?: false)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // Root structure: prefix is already correct, currentKey is empty initially.
        // Nested structure: currentKey has been set by decodeElementIndex.
        val nestedPrefix = currentKey.ifEmpty { prefix }
        return SettingsStoreDecoder(store, nestedPrefix, defaults)
    }

    private fun logIfMissing(key: String) {
        if (!store.hasKey(key)) {
            println("[Settings] Key not found: $key, using default: ${defaults[key]}")
        }
    }
}

// ─── Capturing Encoder: captures all key→value as strings (for defaults / resetField) ───

@OptIn(ExperimentalSerializationApi::class)
private class CapturingEncoder(
    private val prefix: String,
    private val target: MutableMap<String, String> = mutableMapOf()
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    val captured: Map<String, String> get() = target
    private var currentKey = ""

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        currentKey = "$prefix.${descriptor.getElementName(index).lowercase()}"
        return true
    }

    override fun encodeInt(value: Int) { target[currentKey] = value.toString() }
    override fun encodeFloat(value: Float) { target[currentKey] = value.toString() }
    override fun encodeString(value: String) { target[currentKey] = value }
    override fun encodeBoolean(value: Boolean) { target[currentKey] = value.toString() }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return if (currentKey.isEmpty()) this
        else CapturingEncoder(currentKey, target)
    }

    override fun endStructure(descriptor: SerialDescriptor) { /* no-op */ }
}
