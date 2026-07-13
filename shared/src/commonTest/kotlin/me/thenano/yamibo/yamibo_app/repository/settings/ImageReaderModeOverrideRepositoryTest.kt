package me.thenano.yamibo.yamibo_app.repository.settings

import io.github.littlesurvival.dto.value.TagId
import io.github.littlesurvival.dto.value.ThreadId
import io.github.littlesurvival.dto.value.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import me.thenano.yamibo.yamibo_app.store.settings.SettingsStore

class ImageReaderModeOverrideRepositoryTest {
    @Test
    fun effectiveReadingModePriorityIsTagThenThreadThenGlobal() {
        assertEquals(
            EffectiveReadingMode(ReadingMode.SCROLL_CONTINUOUS, EffectiveReadingModeSource.CatalogLongStrip),
            resolveEffectiveReadingMode(
                global = ReadingMode.SINGLE_RTL,
                catalogLongStrip = true,
                threadOverride = ReadingMode.SINGLE_LTR,
            ),
        )
        assertEquals(
            EffectiveReadingMode(ReadingMode.SINGLE_TTB, EffectiveReadingModeSource.ThreadOverride),
            resolveEffectiveReadingMode(
                global = ReadingMode.SINGLE_RTL,
                catalogLongStrip = false,
                threadOverride = ReadingMode.SINGLE_TTB,
            ),
        )
        assertEquals(
            EffectiveReadingMode(ReadingMode.SCROLL_GAP, EffectiveReadingModeSource.Global),
            resolveEffectiveReadingMode(
                global = ReadingMode.SCROLL_GAP,
                catalogLongStrip = false,
                threadOverride = null,
            ),
        )
    }

    @Test
    fun tagLongStripDefaultsOffAndPersists() {
        val store = MemorySettingsStore()
        val repo = SettingsImageReaderModeOverrideRepository(store)
        val tagId = TagId(18666)

        assertFalse(repo.isTagLongStripEnabled(tagId))

        repo.setTagLongStrip(tagId, true)
        assertTrue(SettingsImageReaderModeOverrideRepository(store).isTagLongStripEnabled(tagId))

        repo.setTagLongStrip(tagId, false)
        assertFalse(SettingsImageReaderModeOverrideRepository(store).isTagLongStripEnabled(tagId))
    }

    @Test
    fun rssLongStripDefaultsOffAndPersists() {
        val store = MemorySettingsStore()
        val repo = SettingsImageReaderModeOverrideRepository(store)
        val subscriptionId = 42L

        assertFalse(repo.isRssLongStripEnabled(subscriptionId))

        repo.setRssLongStrip(subscriptionId, true)
        assertTrue(SettingsImageReaderModeOverrideRepository(store).isRssLongStripEnabled(subscriptionId))

        repo.setRssLongStrip(subscriptionId, false)
        assertFalse(SettingsImageReaderModeOverrideRepository(store).isRssLongStripEnabled(subscriptionId))
    }

    @Test
    fun threadModePersistsAndCanBeCleared() {
        val store = MemorySettingsStore()
        val repo = SettingsImageReaderModeOverrideRepository(store)
        val tid = ThreadId(523359)
        val authorId = UserId(123)

        assertNull(repo.getThreadMode(tid, authorId))

        repo.setThreadMode(tid, authorId, ReadingMode.SCROLL_CONTINUOUS)
        assertEquals(
            ReadingMode.SCROLL_CONTINUOUS,
            SettingsImageReaderModeOverrideRepository(store).getThreadMode(tid, authorId),
        )

        repo.setThreadMode(tid, authorId, null)
        assertNull(SettingsImageReaderModeOverrideRepository(store).getThreadMode(tid, authorId))
    }
}

private class MemorySettingsStore : SettingsStore {
    private val values = mutableMapOf<String, String>()

    override fun getInt(key: String, defaultValue: Int): Int = values[key]?.toIntOrNull() ?: defaultValue
    override fun putInt(key: String, value: Int) {
        values[key] = value.toString()
    }

    override fun getFloat(key: String, defaultValue: Float): Float = values[key]?.toFloatOrNull() ?: defaultValue
    override fun putFloat(key: String, value: Float) {
        values[key] = value.toString()
    }

    override fun getString(key: String, defaultValue: String): String = values[key] ?: defaultValue
    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key]?.toBooleanStrictOrNull() ?: defaultValue
    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value.toString()
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun hasKey(key: String): Boolean = key in values
}
