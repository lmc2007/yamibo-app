package me.thenano.yamibo.yamibo_app.repository

/**
 * Theme color scheme using raw hex color values.
 *
 * Stored as Long so the shared module doesn't depend on Compose. The composeApp layer converts
 * these to Compose Color objects.
 */
data class YamiboColorScheme(
        /** Deep brown for headers, primary actions */
        val brownDeep: Long = 0xFF4E2A1B,

        /** Primary brown for accents, text emphasis */
        val brownPrimary: Long = 0xFF6D3A2B,

        /** Light brown for borders, secondary elements */
        val brownLight: Long = 0xFFCCB8A8,

        /** Main page background */
        val creamBackground: Long = 0xFFFFF3D6,

        /** Card / surface background */
        val creamSurface: Long = 0xFFFFF7E0,

        /** Orange accent for highlights, badges, active indicators */
        val orangeAccent: Long = 0xFFF59E2A,

        /** Primary text on light backgrounds */
        val textDark: Long = 0xFF2E1A0E,

        /** Red accent for stats, warnings */
        val redAccent: Long = 0xFFFF5656,

        /** Pinned item background */
        val pinnedBg: Long = 0xFFFFF0C8,

        /** Announcement background */
        val announceBg: Long = 0xFFFFE8B0,
)

/**
 * Theme repository interface.
 *
 * Provides the current color scheme for the app. Future: dark mode, user-customizable themes,
 * persistent theme preferences.
 */
interface ThemeRepository {
    /** Get the current color scheme */
    fun getColorScheme(): YamiboColorScheme
}
