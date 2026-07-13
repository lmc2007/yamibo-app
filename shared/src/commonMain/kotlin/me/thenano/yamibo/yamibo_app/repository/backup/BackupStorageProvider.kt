package me.thenano.yamibo.yamibo_app.repository.backup

import me.thenano.yamibo.yamibo_app.repository.BackupRepository

interface BackupStorageProvider {
    suspend fun getSelectedFolderLabel(): String?
    suspend fun setSelectedFolder(uri: String): Result<Unit>
    suspend fun writeBackupFile(fileName: String, bytes: ByteArray): Result<BackupRepository.BackupFileInfo>
    suspend fun readBackupFile(sourceUri: String): Result<ByteArray>
    suspend fun listBackupFiles(): List<BackupRepository.BackupFileInfo>
    suspend fun getBackupStorageBytes(): Long
    suspend fun deleteBackupFile(fileInfo: BackupRepository.BackupFileInfo): Result<Unit>
}
