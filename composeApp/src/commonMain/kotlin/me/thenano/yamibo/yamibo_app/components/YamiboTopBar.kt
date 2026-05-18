package me.thenano.yamibo.yamibo_app.components

import me.thenano.yamibo.yamibo_app.i18n.appString
import yamibo_app.composeapp.generated.resources.Res
import yamibo_app.composeapp.generated.resources.*

import YamiboIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

/**
 * Standard cream top bar for MainScreen tabs.
 *
 * Use this for top-level tabs such as Reading History, Messages, and Favorites
 * where the header sits directly below the system status bar instead of using
 * the nested brown [YamiboTopBar]. It fixes the content row to 60dp so titles,
 * chips, avatar buttons, and icon buttons keep the same vertical center while
 * switching tabs.
 *
 * Typical usage:
 * ```
 * YamiboMainTabTopBar(
 *     title = appString(Res.string.read_history_title),
 *     actions = { YamiboMainTabIconAction(YamiboIcons.Search, appString(Res.string.read_history_search), onSearch) }
 * )
 * ```
 *
 * @param title Main tab title.
 * @param modifier Optional root modifier.
 * @param actions Right-side actions. Keep items 36dp touch targets or compact
 * chips/buttons so they align to the standard center line.
 */
@Composable
fun YamiboMainTabTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = YamiboTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp),
        color = colors.creamBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.brownDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * Standard icon action for [YamiboMainTabTopBar].
 *
 * Use this for main-tab top-bar icons such as search, delete, refresh, or menu.
 * It keeps a 36dp touch target and a centered icon so tab switches do not cause
 * action controls to visually jump.
 *
 * @param icon Icon vector to draw.
 * @param contentDescription Accessibility label for the action.
 * @param onClick Action invoked when tapped.
 * @param iconSize Visual icon size in dp.
 * @param iconOffsetY Extra vertical optical adjustment for icons whose vector
 * bounds are visually off-center, such as search.
 */
@Composable
fun YamiboMainTabIconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    iconSize: Int = 27,
    iconOffsetY: Int = 0,
) {
    val colors = YamiboTheme.colors
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.brownDeep,
            modifier = Modifier
                .size(iconSize.dp)
                .offset(y = iconOffsetY.dp),
        )
    }
}

/**
 * Standard Yamibo nested-page top bar.
 *
 * Use this for pages that live above a main tab screen and need the app's brown
 * header style, such as user-space subpages, blog reader, private-message
 * reader, WebView-like screens, and thread/detail screens. It centralizes the
 * status-bar padding, left back button sizing, title typography, and right-side
 * action alignment so feature pages do not drift visually.
 *
 * Typical usage:
 * ```
 * YamiboTopBar(
 *     title = appString(Res.string.ui_blog),
 *     onBack = navigator::pop,
 *     actions = { YamiboTopBarIconAction(YamiboIcons.Home, appString(Res.string.main_home), navigator::popToRoot) }
 * )
 * ```
 *
 * @param title Text shown in the header.
 * @param modifier Optional external modifier for the root surface.
 * @param applyStatusPadding Whether to apply status-bar padding. Set false only
 * when the parent already handles system insets.
 * @param titleAlign Use [TextAlign.Center] for chat/reader headers where the
 * title should be visually centered between actions.
 * @param titleFontSize Header title size. Most nested pages use 20sp; compact
 * chat headers can use 18sp.
 * @param onBack Optional back action. When null, no back affordance is shown.
 * @param actions Right-side icon/button content. Keep this short; large controls
 * should live below the top bar.
 */
@Composable
fun YamiboTopBar(
    title: String,
    modifier: Modifier = Modifier,
    applyStatusPadding: Boolean = true,
    titleAlign: TextAlign = TextAlign.Start,
    titleFontSize: Int = 20,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = YamiboTheme.colors
    val rootModifier = if (applyStatusPadding) {
        modifier.fillMaxWidth().statusBarsPadding()
    } else {
        modifier.fillMaxWidth()
    }
    Surface(modifier = rootModifier, color = colors.brownDeep, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
                }
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f).padding(horizontal = if (titleAlign == TextAlign.Center) 8.dp else 4.dp),
                color = Color.White,
                fontSize = titleFontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = titleAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/**
 * Standard icon action for [YamiboTopBar].
 *
 * Use this inside the `actions` slot when a header action maps to a single
 * icon, for example edit, home, search, delete, or overflow. It keeps touch
 * target and icon size aligned across top bars.
 *
 * @param icon Icon vector to draw.
 * @param contentDescription Accessibility label for the action.
 * @param onClick Action invoked when the icon is tapped.
 * @param iconSize Visual icon size in dp. The touch target remains 36dp.
 */
@Composable
fun YamiboTopBarIconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    iconSize: Int = 24,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

