package me.thenano.yamibo.yamibo_app.repository.localnovel

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class PlatformFileOperations(
    private val context: Context,
) {
    actual fun readFileBytes(uri: String): ByteArray {
        val androidUri = android.net.Uri.parse(uri)
        context.contentResolver.openInputStream(androidUri)?.use { input ->
            return input.readBytes()
        } ?: throw IllegalStateException("Cannot open file: $uri")
    }

    actual fun readFileText(uri: String, encoding: String): String {
        val bytes = readFileBytes(uri)
        return bytes.toString(charset(encoding))
    }

    actual fun extractZipToDir(zipUri: String, outputDir: String) {
        val androidUri = android.net.Uri.parse(zipUri)
        val outDir = File(outputDir)
        outDir.mkdirs()

        context.contentResolver.openInputStream(androidUri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryFile = File(outDir, entry.name)
                    if (entry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()
                        FileOutputStream(entryFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: throw IllegalStateException("Cannot open zip file: $zipUri")
    }

    actual fun copyFileToInternal(uri: String, internalPath: String): Boolean {
        return try {
            val bytes = readFileBytes(uri)
            val file = File(internalPath)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun readLocalFileText(path: String): String {
        return File(path).readText()
    }

    actual fun localFileExists(path: String): Boolean {
        return File(path).exists()
    }

    actual fun deleteDirectory(dir: String) {
        File(dir).deleteRecursively()
    }

    actual fun getInternalFilesDir(): String {
        return context.filesDir.absolutePath
    }
}
