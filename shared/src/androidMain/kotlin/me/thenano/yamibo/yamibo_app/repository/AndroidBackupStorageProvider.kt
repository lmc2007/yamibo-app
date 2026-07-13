package me.thenano.yamibo.yamibo_app.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import me.thenano.yamibo.yamibo_app.repository.backup.BackupStorageProvider
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository

class AndroidBackupStorageProvider(
    context: Context,
    private val appSettingsRepository: AppSettingsRepository,
) : BackupStorageProvider {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    override suspend fun getSelectedFolderLabel(): String? {
        val uri = selectedTreeUri() ?: return null
        return queryDisplayName(rootDocumentUri(uri)) ?: "YamiboApp"
    }

    override suspend fun setSelectedFolder(uri: String): Result<Unit> = runCatching {
        val parsed = Uri.parse(uri)
        runCatching {
            resolver.takePersistableUriPermission(
                parsed,
                IntentFlags.READ_WRITE,
            )
        }
        appSettingsRepository.backupFolderUri.setValue(uri)
    }.map { Unit }

    override suspend fun writeBackupFile(
        fileName: String,
        bytes: ByteArray,
    ): Result<BackupRepository.BackupFileInfo> = runCatching {
        val treeUri = selectedTreeUri() ?: error("尚未選擇備份資料夾")
        val fileUri = DocumentsContract.createDocument(
            resolver,
            rootDocumentUri(treeUri),
            BACKUP_MIME_TYPE,
            fileName,
        ) ?: error("無法建立備份檔案")
        resolver.openOutputStream(fileUri, "wt")?.use { it.write(bytes) }
            ?: error("無法寫入備份檔案")
        BackupRepository.BackupFileInfo(
            name = fileName,
            bytes = bytes.size.toLong(),
            uri = fileUri.toString(),
            automatic = fileName.endsWith(AUTO_BACKUP_SUFFIX),
            modifiedAt = null,
        )
    }

    override suspend fun readBackupFile(sourceUri: String): Result<ByteArray> = runCatching {
        resolver.openInputStream(Uri.parse(sourceUri))?.use { it.readBytes() }
            ?: error("無法讀取備份檔案")
    }

    override suspend fun listBackupFiles(): List<BackupRepository.BackupFileInfo> {
        val treeUri = selectedTreeUri() ?: return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val items = mutableListOf<BackupRepository.BackupFileInfo>()
        resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex).orEmpty()
                if (!name.endsWith(BACKUP_EXTENSION)) continue
                val docId = cursor.getString(idIndex)
                val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                items += BackupRepository.BackupFileInfo(
                    name = name,
                    bytes = cursor.getLongOrZero(sizeIndex),
                    uri = fileUri.toString(),
                    automatic = name.endsWith(AUTO_BACKUP_SUFFIX),
                    modifiedAt = cursor.getLongOrNull(modifiedIndex),
                )
            }
        }
        return items
    }

    override suspend fun getBackupStorageBytes(): Long =
        listBackupFiles().sumOf { it.bytes }

    override suspend fun deleteBackupFile(fileInfo: BackupRepository.BackupFileInfo): Result<Unit> = runCatching {
        DocumentsContract.deleteDocument(resolver, Uri.parse(fileInfo.uri))
    }.map { Unit }

    private fun selectedTreeUri(): Uri? =
        appSettingsRepository.backupFolderUri.getValue().takeIf { it.isNotBlank() }?.let(Uri::parse)

    private fun rootDocumentUri(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    private fun queryDisplayName(uri: Uri): String? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun android.database.Cursor.getLongOrZero(index: Int): Long =
        if (index >= 0 && !isNull(index)) getLong(index) else 0L

    private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
        if (index >= 0 && !isNull(index)) getLong(index) else null

    private object IntentFlags {
        const val READ_WRITE: Int =
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }

    private companion object {
        const val BACKUP_EXTENSION = ".yamibobak"
        const val AUTO_BACKUP_SUFFIX = "-autobackup.yamibobak"
        const val BACKUP_MIME_TYPE = "application/octet-stream"
    }
}
