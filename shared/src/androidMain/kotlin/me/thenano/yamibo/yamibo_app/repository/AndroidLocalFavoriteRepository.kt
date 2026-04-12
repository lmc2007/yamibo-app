package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory

class AndroidLocalFavoriteRepository(
    dbFactory: DatabaseFactory
) : LocalFavoriteRepository by LocalFavoriteRepository.LocalFavoriteRepositoryImpl(
    db = Database(dbFactory.createDriver())
)
