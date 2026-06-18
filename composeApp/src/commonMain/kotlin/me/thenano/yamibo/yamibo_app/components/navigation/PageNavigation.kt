package me.thenano.yamibo.yamibo_app.components.navigation

import me.thenano.yamibo.yamibo_app.i18n.i18n

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.littlesurvival.dto.model.PageNav
import me.thenano.yamibo.yamibo_app.forum.components.PageNavigation
import me.thenano.yamibo.yamibo_app.components.theme.YamiboTheme

/**
 * Unified page navigation for both full and prev/next-only pages.
 *
 * Use this whenever a page is backed by `PageNav`. If `PageNav` contains
 * `currentPage` and `totalPages`, the existing full page selector is used. If a
 * Discuz page only exposes previous/next links, this falls back to compact
 * i18n("上一頁 / 下一頁") buttons using `prevPageIndex` / `nextPageIndex` when
 * available.
 *
 * This is intended for ForumPage, UserSpace subpages, BlogReader comments, and
 * PrivateMessage pages so pagination behavior stays consistent.
 *
 * @param pageNav Parsed page navigation DTO.
 * @param currentPage App-side fallback current page when the DTO does not carry
 * a current page value.
 * @param onPageChange Called with the target page number.
 */
@Composable
fun YamiboPageNavigation(
    pageNav: PageNav,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
) {
    val current = pageNav.currentPage
    val total = pageNav.totalPages
    if (current != null && total != null) {
        PageNavigation(pageNav = pageNav, onPageChange = onPageChange)
        return
    }

    val colors = YamiboTheme.colors
    val prevPage = pageNav.prevPageIndex ?: (currentPage - 1).takeIf { pageNav.prevUrl != null && it >= 1 }
    val nextPage = pageNav.nextPageIndex ?: (currentPage + 1).takeIf { pageNav.nextUrl != null }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        YamiboNavButton(i18n("上一頁"), enabled = prevPage != null) {
            prevPage?.let(onPageChange)
        }
        Spacer(Modifier.width(14.dp))
        YamiboNavButton(i18n("下一頁"), enabled = nextPage != null) {
            nextPage?.let(onPageChange)
        }
    }
}

@Composable
private fun YamiboNavButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = YamiboTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) colors.creamSurface else colors.brownLight.copy(alpha = 0.3f),
        shadowElevation = if (enabled) 2.dp else 0.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            color = if (enabled) colors.brownDeep else colors.brownLight,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

