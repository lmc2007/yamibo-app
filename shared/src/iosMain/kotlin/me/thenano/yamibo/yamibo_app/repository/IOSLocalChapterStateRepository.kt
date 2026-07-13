package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.chapterstate.ChapterStateRepositoryImpl

class IOSLocalChapterStateRepository(
    dbFactory: DatabaseFactory,
) : ChapterStateRepository by ChapterStateRepositoryImpl(
    db = Database(dbFactory.createDriver()),
)
