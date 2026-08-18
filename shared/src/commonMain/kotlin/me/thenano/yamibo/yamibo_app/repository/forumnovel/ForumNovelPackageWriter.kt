package me.thenano.yamibo.yamibo_app.repository.forumnovel

data class ForumNovelPackageZipEntry(
    val path: String,
    val bytes: ByteArray,
)

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class ForumNovelPackageWriter {
    fun writeZip(zipPath: String, entries: List<ForumNovelPackageZipEntry>)
}
