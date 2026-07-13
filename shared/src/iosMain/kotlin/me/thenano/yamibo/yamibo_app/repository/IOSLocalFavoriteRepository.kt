package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.favorite.FavoriteStoreRepositoryImpl

class IOSLocalFavoriteRepository(
    dbFactory: DatabaseFactory
) : FavoriteStoreRepository by FavoriteStoreRepositoryImpl(
    db = Database(dbFactory.createDriver())
)
