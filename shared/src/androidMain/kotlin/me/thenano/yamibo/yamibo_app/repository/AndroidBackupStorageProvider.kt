package me.thenano.yamibo.yamibo_app.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import me.thenano.yamibo.yamibo_app.Logger
import me.thenano.yamibo.yamibo_app.repository.backup.BackupStorageProvider
import me.thenano.yamibo.yamibo_app.repository.download.isTransientStorageFailure
import me.thenano.yamibo.yamibo_app.repository.settings.AppSettingsRepository

class AndroidBackupStorageProvider(
    context: Context,
    private val appSettingsRepository: AppSettingsRepository,
) : BackupStorageProvider {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    private inline fun <T> backupResult(operation: String, block: () -> T): Result<T> =
        runCatching(block)
            .onFailure { Logger.e(TAG, "$operation failed", it) }

    override suspend fun getSelectedFolderLabel(): String? = when (val location = resolveLocation()) {
        is BackupLocation.Tree -> queryDisplayName(rootDocumentUri(location.uri)) ?: "YamiboApp"
        BackupLocation.DefaultFolder -> DEFAULT_FOLDER_LABEL
        null -> null
    }

    @SuppressLint("UseKtx")
    override suspend fun setSelectedFolder(uri: String): Result<Unit> = backupResult("setSelectedFolder") {
        val parsed = Uri.parse(uri)
        val alreadyGranted = resolver.persistedUriPermissions.any {
            it.uri == parsed && it.isReadPermission && it.isWritePermission
        }
        if (!alreadyGranted) {
            runCatching {
                resolver.takePersistableUriPermission(
                    parsed,
                    IntentFlags.READ_WRITE,
                )
            }.getOrElse { error ->
                // 授權無法持久化：即使現在可用，重啟後也會失效並在 UI 執行緒拋
                // SecurityException 閃退，因此拒絕接受此資料夾而不是殘留一個失效 URI。
                throw IllegalStateException(
                    "無法長期保存此資料夾的存取權限（${error.message ?: error::class.simpleName}），請改用其他資料夾",
                )
            }
        }
        appSettingsRepository.backupFolderUri.setValue(uri)
    }.map { }

    override suspend fun writeBackupFile(
        fileName: String,
        bytes: ByteArray,
    ): Result<BackupRepository.BackupFileInfo> = backupResult("writeBackupFile fileName=$fileName") {
        when (val location = resolveLocation()) {
            is BackupLocation.Tree -> writeToTree(location.uri, fileName, bytes)
            BackupLocation.DefaultFolder -> writeToDefaultFolder(fileName, bytes)
                ?: error("無法寫入預設備份資料夾（$DEFAULT_FOLDER_LABEL）")
            null -> error("尚未選擇備份資料夾")
        }
    }

    @SuppressLint("UseKtx")
    override suspend fun readBackupFile(sourceUri: String): Result<ByteArray> = backupResult("readBackupFile") {
        resolver.openInputStream(Uri.parse(sourceUri))?.use { it.readBytes() }
            ?: error("無法讀取備份檔案")
    }

    override suspend fun listBackupFiles(): List<BackupRepository.BackupFileInfo> = when (val location = resolveLocation()) {
        is BackupLocation.Tree -> listFromTree(location.uri)
        BackupLocation.DefaultFolder -> listFromDefaultFolder()
        null -> emptyList()
    }

    override suspend fun getBackupStorageBytes(): Long =
        listBackupFiles().sumOf { it.bytes }

    override suspend fun deleteBackupFile(fileInfo: BackupRepository.BackupFileInfo): Result<Unit> =
        backupResult("deleteBackupFile name=${fileInfo.name}") {
            deleteFileByUri(fileInfo)
        }.map { }

    /**
     * 回傳使用者選擇的資料夾 URI；若持久化授權已失效（重裝/還原後 URI 殘留、
     * 授權遺失），視同未選擇，避免 SAF 查詢拋 SecurityException 閃退。
     */
    private fun selectedTreeUri(): Uri? {
        val value = appSettingsRepository.backupFolderUri.getValue().takeIf { it.isNotBlank() } ?: return null
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val hasPersistedGrant = resolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission && it.isWritePermission
        }
        if (!hasPersistedGrant) {
            Logger.w(TAG, "Selected backup folder lost its URI grant; treating as unselected uri=$uri")
            return null
        }
        return uri
    }

    /**
     * 解析當前有效備份位置：優先使用者透過 SAF 選擇的資料夾；未選擇（或授權失效）時
     * 回退到預設公共下載目錄 Download/Yamibo（API 29+，MediaStore 會自動建立目錄）。
     */
    private fun resolveLocation(): BackupLocation? {
        selectedTreeUri()?.let { return BackupLocation.Tree(it) }
        return if (isDefaultFolderSupported()) BackupLocation.DefaultFolder else null
    }

    private fun writeToTree(
        treeUri: Uri,
        fileName: String,
        bytes: ByteArray,
    ): BackupRepository.BackupFileInfo {
        val fileUri = DocumentsContract.createDocument(
            resolver,
            rootDocumentUri(treeUri),
            BACKUP_MIME_TYPE,
            fileName,
        ) ?: error("無法建立備份檔案")
        resolver.openOutputStream(fileUri, "wt")?.use { it.write(bytes) }
            ?: error("無法寫入備份檔案")
        return BackupRepository.BackupFileInfo(
            name = fileName,
            bytes = bytes.size.toLong(),
            uri = fileUri.toString(),
            automatic = fileName.endsWith(AUTO_BACKUP_SUFFIX),
            modifiedAt = null,
        )
    }

    private fun writeToDefaultFolder(
        fileName: String,
        bytes: ByteArray,
    ): BackupRepository.BackupFileInfo? {
        if (!isDefaultFolderSupported()) return null
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, BACKUP_MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, DEFAULT_RELATIVE_PATH)
        }
        val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(fileUri, "wt")?.use { it.write(bytes) } ?: return null
        return BackupRepository.BackupFileInfo(
            name = fileName,
            bytes = bytes.size.toLong(),
            uri = fileUri.toString(),
            automatic = fileName.endsWith(AUTO_BACKUP_SUFFIX),
            modifiedAt = null,
        )
    }

    private fun listFromTree(treeUri: Uri): List<BackupRepository.BackupFileInfo> {
        return try {
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
            items
        } catch (error: Exception) {
            if (!error.isTransientStorageFailure()) throw error
            Logger.w(TAG, "Storage unavailable while listing backup files uri=$treeUri", error)
            emptyList()
        }
    }

    private fun listFromDefaultFolder(): List<BackupRepository.BackupFileInfo> {
        if (!isDefaultFolderSupported()) return emptyList()
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(DEFAULT_RELATIVE_PATH)
            val items = mutableListOf<BackupRepository.BackupFileInfo>()
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val modifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex).orEmpty()
                    if (!name.endsWith(BACKUP_EXTENSION)) continue
                    val fileUri = ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idIndex),
                    )
                    items += BackupRepository.BackupFileInfo(
                        name = name,
                        bytes = cursor.getLongOrZero(sizeIndex),
                        uri = fileUri.toString(),
                        automatic = name.endsWith(AUTO_BACKUP_SUFFIX),
                        // MediaStore DATE_MODIFIED 以秒為單位，轉換為毫秒與 SAF 一致
                        modifiedAt = cursor.getLongOrNull(modifiedIndex)?.times(1000L),
                    )
                }
            }
            items
        } catch (error: Exception) {
            if (!error.isTransientStorageFailure()) throw error
            Logger.w(TAG, "Storage unavailable while listing default backup folder", error)
            emptyList()
        }
    }

    private fun deleteFileByUri(fileInfo: BackupRepository.BackupFileInfo) {
        val uri = Uri.parse(fileInfo.uri)
        if (uri.authority == MediaStore.AUTHORITY) {
            resolver.delete(uri, null, null)
        } else {
            DocumentsContract.deleteDocument(resolver, uri)
        }
    }

    private fun rootDocumentUri(treeUri: Uri): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))

    private fun queryDisplayName(uri: Uri): String? =
        runCatching {
            val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()

    private fun android.database.Cursor.getLongOrZero(index: Int): Long =
        if (index >= 0 && !isNull(index)) getLong(index) else 0L

    private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
        if (index >= 0 && !isNull(index)) getLong(index) else null

    private object IntentFlags {
        const val READ_WRITE: Int =
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }

    private sealed interface BackupLocation {
        data class Tree(val uri: Uri) : BackupLocation
        data object DefaultFolder : BackupLocation
    }

    companion object {
        /** 預設備份資料夾是否可用（MediaStore Downloads 集合需要 API 29+） */
        fun isDefaultFolderSupported(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        private const val TAG = "AndroidBackupStorageProvider"
        private const val BACKUP_EXTENSION = ".yamibobak"
        private const val AUTO_BACKUP_SUFFIX = "-autobackup.yamibobak"
        private const val BACKUP_MIME_TYPE = "application/octet-stream"
        const val DEFAULT_FOLDER_LABEL = "Download/Yamibo"

        /** 公共下載目錄中的預設備份資料夾（尾斜杠與 MediaStore RELATIVE_PATH 格式一致） */
        val DEFAULT_RELATIVE_PATH: String = Environment.DIRECTORY_DOWNLOADS + "/Yamibo/"
    }
}
