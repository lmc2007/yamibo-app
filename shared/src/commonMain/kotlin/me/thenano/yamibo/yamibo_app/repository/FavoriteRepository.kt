package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.core.YamiboResult
import io.github.littlesurvival.dto.page.FavoritePage
import io.github.littlesurvival.dto.page.FavoriteType
import io.github.littlesurvival.dto.value.FavoriteId
import io.github.littlesurvival.dto.value.FormHash
import io.github.littlesurvival.dto.value.UserId

interface FavoriteRepository {
    suspend fun fetchFavorites(
        userId: UserId? = null,
        type: FavoriteType = FavoriteType.Thread,
        page: Int = 1
    ): YamiboResult<FavoritePage>

    suspend fun removeFavorite(
        favoriteId: FavoriteId,
        formHash: FormHash,
    ): YamiboResult<String>
}
