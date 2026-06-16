package me.thenano.yamibo.yamibo_app.components.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard cream top bar for MainScreen tabs.
 *
 * Use this for top-level tabs such as Reading History, Messages, and Favorites
 * where the header sits directly below the system status bar instead of using
 * the nested brown [YamiboTopBar]. It fixes the content row to 60dp so titles,
 * chips, avatar buttons, and icon buttons keep the same vertical center while
 * switching tabs.
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
    YamiboAppBar(
        title = title,
        style = YamiboAppBarStyle.MainTabCream,
        modifier = modifier,
        navigationWidth = 0.dp,
        actions = actions,
    )
}
