package me.thenano.yamibo.yamibo_app.components.navigation

import YamiboIcons
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard Yamibo nested-page top bar.
 *
 * Use this for pages that live above a main tab screen and need the app's brown
 * header style, such as user-space subpages, blog reader, private-message
 * reader, WebView-like screens, and thread/detail screens. It centralizes the
 * status-bar padding, left back button sizing, title typography, and right-side
 * action alignment so feature pages do not drift visually.
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
    YamiboAppBar(
        title = title,
        style = YamiboAppBarStyle.NestedBrown,
        modifier = modifier,
        applyStatusPadding = applyStatusPadding,
        titleAlign = titleAlign,
        titleFontSize = titleFontSize,
        navigationWidth = if (onBack == null) 0.dp else 44.dp,
        navigation = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Text(YamiboIcons.Back, color = Color.White, fontSize = 20.sp)
                }
            }
        },
        actions = actions,
    )
}
