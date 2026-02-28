package me.thenano.yamibo.yamibo_app.store

interface AppSettingStore {
    suspend fun load()
    suspend fun save()
}