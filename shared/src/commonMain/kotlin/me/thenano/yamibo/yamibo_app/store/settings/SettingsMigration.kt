package me.thenano.yamibo.yamibo_app.store.settings

/**
 * Describes a single key migration step.
 *
 * @param version Target version number (monotonically increasing).
 * @param renames Map of oldKey → newKey to migrate.
 * @param deletions Set of keys to delete outright.
 */
data class SettingsMigration(
    val version: Int,
    val renames: Map<String, String> = emptyMap(),
    val deletions: Set<String> = emptySet()
)
