package me.thenano.yamibo.yamibo_app.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.FavoritePage
import me.thenano.yamibo.yamibo_app.LocalFavoriteRepository
import me.thenano.yamibo.yamibo_app.theme.YamiboTheme

@Composable
fun FavoritePage() {
    val colors = YamiboTheme.colors
    val favoriteRepository = LocalFavoriteRepository.current
    var favoritePage by remember { mutableStateOf<FavoritePage?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        when (val result = favoriteRepository.fetchFavorites()) {
            is YamiboResult.Success -> favoritePage = result.value
            else -> {}
        }
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize().background(colors.creamBackground),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Text("載入收藏列表中...", color = colors.brownDeep)
        } else if (favoritePage?.items.isNullOrEmpty()) {
            Text("目前沒有任何收藏", color = colors.brownDeep)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(favoritePage?.items ?: emptyList()) { item ->
                    Text(
                        text = item.name,
                        color = colors.brownDeep,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
