package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.chapterstate.LocalChapterStateRepositoryImpl

class AndroidLocalChapterStateRepository(
    dbFactory: DatabaseFactory,
) : LocalChapterStateRepository by LocalChapterStateRepositoryImpl(
    db = Database(dbFactory.createDriver()),
)
