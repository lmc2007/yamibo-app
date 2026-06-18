package me.thenano.yamibo.yamibo_app.repository.font

class IOSFontPlatform : FontPlatform {
    override val supportsFontLoading: Boolean = false
    override val unavailableMessage: String = "Dynamic font loading is not available on iOS yet."

    override suspend fun importFont(sourceUri: String, displayName: String?, id: String): FontImportResult =
        FontImportResult.Unsupported(unavailableMessage)

    override fun deleteFont(font: LoadedFont): Boolean = false
}
