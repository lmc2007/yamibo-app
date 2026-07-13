package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.bookmark.BookMarkRepositoryImpl

class AndroidLocalBookMarkRepository(
    dbFactory: DatabaseFactory,
) : BookMarkRepository by BookMarkRepositoryImpl(
    db = Database(dbFactory.createDriver()),
)
