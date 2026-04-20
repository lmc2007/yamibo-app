package me.thenano.yamibo.yamibo_app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import me.thenano.yamibo.yamibo_app.LocalAppSettingsRepository
import me.thenano.yamibo.yamibo_app.util.state

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
    val htmlTextDark: Color,
    val redAccent: Color,
    val pinnedBg: Color,
    val announceBg: Color,
    val navBarBg: Color,
    val navBarIconSelected: Color,
    val navBarIconUnselected: Color,
)
/** Central theme object. Access colors via `YamiboTheme.colors`. */
object YamiboTheme {
    val colors: YamiboColors
        @Composable
        get() {
            val schemeEnum = LocalAppSettingsRepository.current.themeScheme.state()
            val scheme = schemeEnum.toScheme()
            
            return YamiboColors(
                brownDeep = Color(scheme.brownDeep),
                brownPrimary = Color(scheme.brownPrimary),
                brownLight = Color(scheme.brownLight),
                creamBackground = Color(scheme.creamBackground),
                creamSurface = Color(scheme.creamSurface),
                orangeAccent = Color(scheme.orangeAccent),
                textDark = Color(scheme.textDark),
                htmlTextDark = Color(scheme.htmlTextDark),
                redAccent = Color(scheme.redAccent),
                pinnedBg = Color(scheme.pinnedBg),
                announceBg = Color(scheme.announceBg),
                navBarBg = Color(scheme.navBarBg),
                navBarIconSelected = Color(scheme.navBarIconSelected),
                navBarIconUnselected = Color(scheme.navBarIconUnselected),
            )
        }
}
