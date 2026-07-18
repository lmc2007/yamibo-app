package me.thenano.yamibo.yamibo_app.repository.localnovel

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.LocalNovelRepository

class IOSLocalNovelRepository(
    dbFactory: DatabaseFactory,
) : LocalNovelRepository by LocalNovelRepositoryImpl(
    db = Database(dbFactory.createDriver())
)
