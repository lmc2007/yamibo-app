package me.thenano.yamibo.yamibo_app.repository.forumnovel

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class ForumNovelPackageWriter {
    actual fun writeZip(zipPath: String, entries: List<ForumNovelPackageZipEntry>) {
        val file = File(zipPath)
        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.path))
                zip.write(entry.bytes)
                zip.closeEntry()
            }
        }
    }
}
