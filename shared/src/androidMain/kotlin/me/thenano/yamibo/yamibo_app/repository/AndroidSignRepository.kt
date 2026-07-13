package me.thenano.yamibo.yamibo_app.repository

import io.github.littlesurvival.YamiboClient
import me.thenano.yamibo.yamibo_app.Database
import me.thenano.yamibo.yamibo_app.db.DatabaseFactory
import me.thenano.yamibo.yamibo_app.repository.sign.SignRepositoryImpl
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository
import me.thenano.yamibo.yamibo_app.store.sign.DatabaseSignStatusStore

class AndroidSignRepository(
    dbFactory: DatabaseFactory,
    authRepository: AuthRepository,
    appSettingsRepository: AppSettingsRepository,
    yamiboClient: YamiboClient,
) : SignRepository by SignRepositoryImpl(
    signStatusStore = DatabaseSignStatusStore(Database(dbFactory.createDriver())),
    authRepository = authRepository,
    appSettingsRepository = appSettingsRepository,
    yamiboClient = yamiboClient,
)
