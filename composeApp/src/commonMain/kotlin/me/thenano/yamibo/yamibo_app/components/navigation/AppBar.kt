package me.thenano.yamibo.yamibo_app.components.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.systembars.SystemBarsEffect
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

enum class YamiboAppBarStyle {
    MainTabCream,
    NestedBrown,
    SearchBrown,
    ReaderOverlay,
}

/**
 * Single low-level app bar used by all standard Yamibo top bars.
 *
 * System bar color ownership belongs here so feature screens do not call
 * [SystemBarsEffect] directly. Feature code should normally use
 * [YamiboTopBar] or [YamiboMainTabTopBar] unless it needs a custom navigation
 * slot.
 */
@Composable
fun YamiboAppBar(
    title: String,
    style: YamiboAppBarStyle,
    modifier: Modifier = Modifier,
    applyStatusPadding: Boolean = true,
    titleAlign: TextAlign = TextAlign.Start,
    titleFontSize: Int? = null,
    navigationWidth: Dp = 44.dp,
    navigation: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = YamiboTheme.colors
    val barColor = when (style) {
        YamiboAppBarStyle.MainTabCream -> colors.creamBackground
        YamiboAppBarStyle.NestedBrown,
        YamiboAppBarStyle.SearchBrown,
        YamiboAppBarStyle.ReaderOverlay -> colors.brownDeep
    }
    val contentColor = when (style) {
        YamiboAppBarStyle.MainTabCream -> colors.textOnBackground
        YamiboAppBarStyle.NestedBrown,
        YamiboAppBarStyle.SearchBrown,
        YamiboAppBarStyle.ReaderOverlay -> Color.White
    }
    val navigationColor = when (style) {
        YamiboAppBarStyle.MainTabCream -> colors.navBarBg
        YamiboAppBarStyle.NestedBrown,
        YamiboAppBarStyle.SearchBrown -> colors.creamBackground
        YamiboAppBarStyle.ReaderOverlay -> barColor
    }
    val priority = when (style) {
        YamiboAppBarStyle.MainTabCream -> 10
        YamiboAppBarStyle.NestedBrown,
        YamiboAppBarStyle.SearchBrown,
        YamiboAppBarStyle.ReaderOverlay -> 100
    }
    val resolvedTitleSize = titleFontSize ?: when (style) {
        YamiboAppBarStyle.MainTabCream -> 20
        YamiboAppBarStyle.NestedBrown,
        YamiboAppBarStyle.SearchBrown,
        YamiboAppBarStyle.ReaderOverlay -> 20
    }

    SystemBarsEffect(
        statusBarColor = barColor,
        navigationBarColor = navigationColor,
        priority = priority,
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = barColor,
        shadowElevation = if (style == YamiboAppBarStyle.MainTabCream) 0.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applyStatusPadding) Modifier.statusBarsPadding() else Modifier)
                .height(60.dp)
                .padding(
                    start = if (style == YamiboAppBarStyle.MainTabCream) 16.dp else 4.dp,
                    end = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationWidth > 0.dp) {
                Row(
                    modifier = Modifier.size(width = navigationWidth, height = 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    content = navigation,
                )
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (titleAlign == TextAlign.Center) 8.dp else 4.dp),
                color = contentColor,
                fontSize = resolvedTitleSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = titleAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(
                    space = if (style == YamiboAppBarStyle.MainTabCream) 4.dp else 0.dp,
                    alignment = Alignment.End,
                ),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
