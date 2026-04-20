package me.thenano.yamibo.yamibo_app.repository.scheme

/**
 * Theme color scheme using raw hex color values (Long).
 *
 * Stored as Long so the shared module doesn't depend on Compose. The composeApp layer converts
 * these to Compose Color objects.
 */
sealed class YamiboColorScheme {
    abstract val name: String

    /** Deep color for headers, primary actions */
    abstract val brownDeep: Long

    /** Primary color for accents, text emphasis */
    abstract val brownPrimary: Long

    /** Light color for borders, secondary elements */
    abstract val brownLight: Long

    /** Main page background */
    abstract val creamBackground: Long

    /** Card / surface background */
    abstract val creamSurface: Long

    /** Accent for highlights, badges, active indicators */
    abstract val orangeAccent: Long

    /** Primary text on backgrounds */
    abstract val textDark: Long

    /** Primary text inside HTML viewer (threads) */
    open val htmlTextDark: Long get() = textDark

    /** Red accent for stats, warnings */
    abstract val redAccent: Long

    /** Pinned item background */
    abstract val pinnedBg: Long

    /** Announcement background */
    abstract val announceBg: Long

    /** Bottom navigation bar background */
    abstract val navBarBg: Long

    /** Bottom navigation active icon color */
    abstract val navBarIconSelected: Long

    /** Bottom navigation inactive icon color */
    abstract val navBarIconUnselected: Long

    /** Default Warm Brown (Light) */
    data object Default : YamiboColorScheme() {
        override val name = "百合會"
        override val brownDeep = 0xFF4E2A1B
        override val brownPrimary = 0xFF6D3A2B
        override val brownLight = 0xFFCCB8A8
        override val creamBackground = 0xFFFFF3D6
        override val creamSurface = 0xFFFFF7E0
        override val orangeAccent = 0xFFF59E2A
        override val textDark = 0xFF2E1A0E
        override val htmlTextDark = 0xFF6E2B19
        override val redAccent = 0xFFFF5656
        override val pinnedBg = 0xFFFFF0C8
        override val announceBg = 0xFFFFE8B0
        override val navBarBg = 0xFFFFE6B7
        override val navBarIconSelected = 0xFF6E2B19
        override val navBarIconUnselected = 0xFFD29D7C
    }

    /** Default Dark — dark version of warm brown */
    data object DefaultDark : YamiboColorScheme() {
        override val name = "百合會(暗色)"
        override val brownDeep = 0xFF2C1810
        override val brownPrimary = 0xFF8B5E3C
        override val brownLight = 0xFF6B5545
        override val creamBackground = 0xFF1A1210
        override val creamSurface = 0xFF2A1E18
        override val orangeAccent = 0xFFF5A623
        override val textDark = 0xFFE8D5C4
        override val redAccent = 0xFFEF5350
        override val pinnedBg = 0xFF2E2218
        override val announceBg = 0xFF332618
        override val navBarBg = 0xFF31211B
        override val navBarIconSelected = 0xFFF5A623
        override val navBarIconUnselected = 0xFF8B5E3C
    }

    /** Classic Black */
    data object ClassicBlack : YamiboColorScheme() {
        override val name = "傳統黑"
        override val brownDeep = 0xFF1A1A1A
        override val brownPrimary = 0xFF555555
        override val brownLight = 0xFF666666
        override val creamBackground = 0xFF121212
        override val creamSurface = 0xFF1E1E1E
        override val orangeAccent = 0xFFBB86FC
        override val textDark = 0xFFE0E0E0
        override val redAccent = 0xFFCF6679
        override val pinnedBg = 0xFF252525
        override val announceBg = 0xFF2A2A2A
        override val navBarBg = 0xFF121212
        override val navBarIconSelected = 0xFFBB86FC
        override val navBarIconUnselected = 0xFF888888
    }

    /** Classic White */
    data object ClassicWhite : YamiboColorScheme() {
        override val name = "傳統白"
        override val brownDeep = 0xFF2C2C2C
        override val brownPrimary = 0xFF555555
        override val brownLight = 0xFFBBBBBB
        override val creamBackground = 0xFFF5F5F5
        override val creamSurface = 0xFFFFFFFF
        override val orangeAccent = 0xFF1976D2
        override val textDark = 0xFF212121
        override val redAccent = 0xFFD32F2F
        override val pinnedBg = 0xFFE3F2FD
        override val announceBg = 0xFFFFF8E1
        override val navBarBg = 0xFFF5F5F5
        override val navBarIconSelected = 0xFF1976D2
        override val navBarIconUnselected = 0xFF888888
    }

    /** Cappuccino Mocha */
    data object Catppuccin : YamiboColorScheme() {
        override val name = "catppuccin"
        override val brownDeep = 0xFF1E1E2E
        override val brownPrimary = 0xFFCBA6F7
        override val brownLight = 0xFF585B70
        override val creamBackground = 0xFF181825
        override val creamSurface = 0xFF313244
        override val orangeAccent = 0xFFF5C2E7
        override val textDark = 0xFFCDD6F4
        override val redAccent = 0xFFF38BA8
        override val pinnedBg = 0xFF2A2A3D
        override val announceBg = 0xFF2E2E42
        override val navBarBg = 0xFF181825
        override val navBarIconSelected = 0xFFF5C2E7
        override val navBarIconUnselected = 0xFF585B70
    }

    /** Green Apple */
    data object GreenApple : YamiboColorScheme() {
        override val name = "Green Apple"
        override val brownDeep = 0xFF0D2818
        override val brownPrimary = 0xFF4CAF50
        override val brownLight = 0xFF2E5E3A
        override val creamBackground = 0xFF0A1F14
        override val creamSurface = 0xFF153324
        override val orangeAccent = 0xFF66BB6A
        override val textDark = 0xFFD4E8D0
        override val redAccent = 0xFFEF5350
        override val pinnedBg = 0xFF1A3828
        override val announceBg = 0xFF1D3E2C
        override val navBarBg = 0xFF0A1F14
        override val navBarIconSelected = 0xFF66BB6A
        override val navBarIconUnselected = 0xFF4CAF50
    }

    /** Lavender */
    data object Lavender : YamiboColorScheme() {
        override val name = "Lavender"
        override val brownDeep = 0xFF1C1528
        override val brownPrimary = 0xFF9C8EC1
        override val brownLight = 0xFF5A4E73
        override val creamBackground = 0xFF15102A
        override val creamSurface = 0xFF231D3A
        override val orangeAccent = 0xFFB39DDB
        override val textDark = 0xFFDBD3F0
        override val redAccent = 0xFFE57373
        override val pinnedBg = 0xFF27204A
        override val announceBg = 0xFF2C254E
        override val navBarBg = 0xFF15102A
        override val navBarIconSelected = 0xFFB39DDB
        override val navBarIconUnselected = 0xFF5A4E73
    }

    /** Midnight Dusk */
    data object MidnightDusk : YamiboColorScheme() {
        override val name = "Midnight Dusk"
        override val brownDeep = 0xFF16101C
        override val brownPrimary = 0xFFC27280
        override val brownLight = 0xFF5C4655
        override val creamBackground = 0xFF201520
        override val creamSurface = 0xFF2D1F2D
        override val orangeAccent = 0xFFF48FB1
        override val textDark = 0xFFE8D0DC
        override val redAccent = 0xFFFF6E8C
        override val pinnedBg = 0xFF322432
        override val announceBg = 0xFF382838
        override val navBarBg = 0xFF201520
        override val navBarIconSelected = 0xFFF48FB1
        override val navBarIconUnselected = 0xFF5C4655
    }

    /** Nord */
    data object Nord : YamiboColorScheme() {
        override val name = "Nord"
        override val brownDeep = 0xFF2E3440
        override val brownPrimary = 0xFF88C0D0
        override val brownLight = 0xFF4C566A
        override val creamBackground = 0xFF242933
        override val creamSurface = 0xFF3B4252
        override val orangeAccent = 0xFF8FBCBB
        override val textDark = 0xFFECEFF4
        override val redAccent = 0xFFBF616A
        override val pinnedBg = 0xFF353D4B
        override val announceBg = 0xFF3A4253
        override val navBarBg = 0xFF242933
        override val navBarIconSelected = 0xFF88C0D0
        override val navBarIconUnselected = 0xFF4C566A
    }

    /** Strawberry Daiquiri */
    data object StrawberryDaiquiri : YamiboColorScheme() {
        override val name = "Strawberry Daiquiri"
        override val brownDeep = 0xFF1C0F14
        override val brownPrimary = 0xFFE05575
        override val brownLight = 0xFF6D3B4D
        override val creamBackground = 0xFF18090F
        override val creamSurface = 0xFF2A141E
        override val orangeAccent = 0xFFFF7799
        override val textDark = 0xFFF0D0DA
        override val redAccent = 0xFFFF5370
        override val pinnedBg = 0xFF2E1822
        override val announceBg = 0xFF331C28
        override val navBarBg = 0xFF18090F
        override val navBarIconSelected = 0xFFE05575
        override val navBarIconUnselected = 0xFF6D3B4D
    }

    /** tako */
    data object Tako : YamiboColorScheme() {
        override val name = "Tako"
        override val brownDeep = 0xFF21212E
        override val brownPrimary = 0xFFF3B375
        override val brownLight = 0xFF52505C
        override val creamBackground = 0xFF2A2A3C
        override val creamSurface = 0xFF36364A
        override val orangeAccent = 0xFFF3B375
        override val textDark = 0xFFE8DED4
        override val redAccent = 0xFFEE6B6B
        override val pinnedBg = 0xFF3A3A50
        override val announceBg = 0xFF3E3E55
        override val navBarBg = 0xFF2A2A3C
        override val navBarIconSelected = 0xFFF3B375
        override val navBarIconUnselected = 0xFF52505C
    }

    /** Teal & Turquoise */
    data object TealTurquoise : YamiboColorScheme() {
        override val name = "Teal & Turquoise"
        override val brownDeep = 0xFF0D1F22
        override val brownPrimary = 0xFF40BFA0
        override val brownLight = 0xFF2A5550
        override val creamBackground = 0xFF0A1A1C
        override val creamSurface = 0xFF152C2E
        override val orangeAccent = 0xFF4DD0B8
        override val textDark = 0xFFD0EDE6
        override val redAccent = 0xFFFF7070
        override val pinnedBg = 0xFF1A3432
        override val announceBg = 0xFF1E3A38
        override val navBarBg = 0xFF0A1A1C
        override val navBarIconSelected = 0xFF4DD0B8
        override val navBarIconUnselected = 0xFF2A5550
    }

    /** Tidal Wave */
    data object TidalWave : YamiboColorScheme() {
        override val name = "Tidal Wave"
        override val brownDeep = 0xFF0D1520
        override val brownPrimary = 0xFF5B9BD5
        override val brownLight = 0xFF2E4A65
        override val creamBackground = 0xFF0A1018
        override val creamSurface = 0xFF152535
        override val orangeAccent = 0xFF64B5F6
        override val textDark = 0xFFD0E0F0
        override val redAccent = 0xFFFF7070
        override val pinnedBg = 0xFF1A2E42
        override val announceBg = 0xFF1E3448
        override val navBarBg = 0xFF0A1018
        override val navBarIconSelected = 0xFF64B5F6
        override val navBarIconUnselected = 0xFF2E4A65
    }

    /** Yin & Yang */
    data object YinYang : YamiboColorScheme() {
        override val name = "Yin & Yang"
        override val brownDeep = 0xFF1E1814
        override val brownPrimary = 0xFFD4A574
        override val brownLight = 0xFF5A4A3A
        override val creamBackground = 0xFF181210
        override val creamSurface = 0xFF282018
        override val orangeAccent = 0xFFE8B87A
        override val textDark = 0xFFE8DDD0
        override val redAccent = 0xFFF06060
        override val pinnedBg = 0xFF2C2418
        override val announceBg = 0xFF30281C
        override val navBarBg = 0xFF181210
        override val navBarIconSelected = 0xFFE8B87A
        override val navBarIconUnselected = 0xFF5A4A3A
    }

    /** Yotsuba */
    data object Yotsuba : YamiboColorScheme() {
        override val name = "Yotsuba"
        override val brownDeep = 0xFF2A1E14
        override val brownPrimary = 0xFFD48850
        override val brownLight = 0xFF6B5040
        override val creamBackground = 0xFF1E1610
        override val creamSurface = 0xFF2E2218
        override val orangeAccent = 0xFFE8A060
        override val textDark = 0xFFF0E0D0
        override val redAccent = 0xFFE85050
        override val pinnedBg = 0xFF342818
        override val announceBg = 0xFF382C1C
        override val navBarBg = 0xFF1E1610
        override val navBarIconSelected = 0xFFE8A060
        override val navBarIconUnselected = 0xFF6B5040
    }

    /** Monochrome */
    data object Monochrome : YamiboColorScheme() {
        override val name = "Monochrome"
        override val brownDeep = 0xFF000000
        override val brownPrimary = 0xFF888888
        override val brownLight = 0xFFAAAAAA
        override val creamBackground = 0xFFFFFFFF
        override val creamSurface = 0xFFF0F0F0
        override val orangeAccent = 0xFF444444
        override val textDark = 0xFF111111
        override val redAccent = 0xFF333333
        override val pinnedBg = 0xFFE8E8E8
        override val announceBg = 0xFFDDDDDD
        override val navBarBg = 0xFFFFFFFF
        override val navBarIconSelected = 0xFF000000
        override val navBarIconUnselected = 0xFFAAAAAA
    }

    companion object {
        /** All available themes, for UI theme picker */
        val all: List<YamiboColorScheme> by lazy {
            listOf(
                Default,
                DefaultDark,
                ClassicBlack,
                ClassicWhite,
                Catppuccin,
                GreenApple,
                Lavender,
                MidnightDusk,
                Nord,
                StrawberryDaiquiri,
                Tako,
                TealTurquoise,
                TidalWave,
                YinYang,
                Yotsuba,
                Monochrome,
            )
        }
    }
}