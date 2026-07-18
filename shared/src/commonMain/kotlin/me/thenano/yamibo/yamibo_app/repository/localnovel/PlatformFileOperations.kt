package me.thenano.yamibo.yamibo_app.repository.localnovel

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class PlatformFileOperations {
    fun readFileBytes(uri: String): ByteArray
    fun extractZipToDir(zipUri: String, outputDir: String)
    fun readFileText(uri: String, encoding: String): String
    fun copyFileToInternal(uri: String, internalPath: String): Boolean
    fun readLocalFileText(path: String): String
    fun localFileExists(path: String): Boolean
    fun deleteDirectory(dir: String)
    fun getInternalFilesDir(): String
}
