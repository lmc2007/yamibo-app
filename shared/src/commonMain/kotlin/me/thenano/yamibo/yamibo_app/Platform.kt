package me.thenano.yamibo.yamibo_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform