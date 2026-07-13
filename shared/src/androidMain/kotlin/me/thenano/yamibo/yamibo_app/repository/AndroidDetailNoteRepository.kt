package me.thenano.yamibo.yamibo_app.repository

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.detailnote.DetailNoteRepositoryImpl

class AndroidDetailNoteRepository(
    dbFactory: DatabaseFactory,
) : DetailNoteRepository by DetailNoteRepositoryImpl(
    db = Database(dbFactory.createDriver()),
)
