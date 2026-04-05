package me.thenano.yamibo.yamibo_app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import me.thenano.yamibo.yamibo_app.LocalThemeRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.repository.scheme.YamiboColorScheme

/** Compose-layer color wrapper. Converts repo Long hex → Compose Color. */
@Immutable
data class YamiboColors(
    val brownDeep: Color,
    val brownPrimary: Color,
    val brownLight: Color,
    val creamBackground: Color,
    val creamSurface: Color,
    val orangeAccent: Color,
    val textDark: Color,
    val redAccent: Color,
    val pinnedBg: Color,
    val announceBg: Color,
)
/** Central theme object. Access colors via `YamiboTheme.colors`. */
object YamiboTheme {
    val colors: YamiboColors
        @Composable
        get() {
            val settings by LocalAppSettingsRepository.current.settings.collectAsState()
            val schemeName = settings.theme.scheme
            val scheme = YamiboColorScheme.all.firstOrNull { it.name == schemeName } ?: YamiboColorScheme.Default
            
            return YamiboColors(
                brownDeep = Color(scheme.brownDeep),
                brownPrimary = Color(scheme.brownPrimary),
                brownLight = Color(scheme.brownLight),
                creamBackground = Color(scheme.creamBackground),
                creamSurface = Color(scheme.creamSurface),
                orangeAccent = Color(scheme.orangeAccent),
                textDark = Color(scheme.textDark),
                redAccent = Color(scheme.redAccent),
                pinnedBg = Color(scheme.pinnedBg),
                announceBg = Color(scheme.announceBg),
            )
        }
}
