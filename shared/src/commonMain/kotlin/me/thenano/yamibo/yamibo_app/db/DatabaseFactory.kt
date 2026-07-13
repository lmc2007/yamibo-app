package me.thenano.yamibo.yamibo_app.db

import app.cash.sqldelight.db.SqlDriver

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class DatabaseFactory {
    fun createDriver(): SqlDriver
}
