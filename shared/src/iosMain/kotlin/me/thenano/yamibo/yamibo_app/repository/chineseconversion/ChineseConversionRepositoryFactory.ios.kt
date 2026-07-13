package me.thenano.yamibo.yamibo_app.repository.chineseconversion

import me.thenano.yamibo.yamibo_app.repository.ChineseConversionRepository
import me.thenano.yamibo.yamibo_app.repository.IOSChineseConversionRepository

actual fun createChineseConversionRepository(): ChineseConversionRepository = IOSChineseConversionRepository()
