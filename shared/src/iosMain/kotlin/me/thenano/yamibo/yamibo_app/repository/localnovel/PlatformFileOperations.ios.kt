package me.thenano.yamibo.yamibo_app.repository.localnovel

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformFileOperations {
    actual fun readFileBytes(uri: String): ByteArray {
        // TODO: iOS implementation using NSData(contentsOfURL:)
        throw UnsupportedOperationException("iOS file reading not yet implemented")
    }

    actual fun readFileText(uri: String, encoding: String): String {
        val bytes = readFileBytes(uri)
        return bytes.toString(charset(encoding))
    }

    actual fun extractZipToDir(zipUri: String, outputDir: String) {
        // TODO: iOS implementation using Foundation ZIP
        throw UnsupportedOperationException("iOS ZIP extraction not yet implemented")
    }

    actual fun copyFileToInternal(uri: String, internalPath: String): Boolean {
        return try {
            val bytes = readFileBytes(uri)
            // TODO: iOS file writing
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun readLocalFileText(path: String): String {
        // TODO: iOS implementation
        throw UnsupportedOperationException("iOS file reading not yet implemented")
    }

    actual fun localFileExists(path: String): Boolean {
        // TODO: iOS implementation
        return false
    }

    actual fun deleteDirectory(dir: String) {
        // TODO: iOS implementation
    }

    actual fun getInternalFilesDir(): String {
        // TODO: iOS implementation
        return ""
    }
}
