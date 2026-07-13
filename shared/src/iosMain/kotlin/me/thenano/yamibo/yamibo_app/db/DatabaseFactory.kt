package me.thenano.yamibo.yamibo_app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.thenano.yamibo.yamibo_app.Database

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class DatabaseFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(Database.Schema, "yamibo.db")
}
