package me.thenano.yamibo.yamibo_app.repository.forumnovel

import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory

class AndroidForumNovelShelfRepository(
    dbFactory: DatabaseFactory,
) : ForumNovelShelfRepository by ForumNovelShelfRepositoryImpl(
    db = Database(dbFactory.createDriver())
)
