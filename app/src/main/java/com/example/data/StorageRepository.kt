package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class FolderScanResult(
    val folders: List<ScannedFolder>,
    val files: List<ScannedFileItem>
)

data class DeleteFilesResult(
    val deletedCount: Int,
    val freedBytes: Long,
    val failedCount: Int
)

class StorageRepository(private val context: Context) {

    fun takePersistablePermission(treeUri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
            true
        } catch (_: SecurityException) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun releasePersistablePermission(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Permission may already be revoked by user/system
        }
    }

    suspend fun scanAuthorizedFolders(
        folderUris: Set<String>,
        onProgress: (folderName: String, scannedCount: Int) -> Unit
    ): FolderScanResult = withContext(Dispatchers.IO) {
        val scannedFolders = mutableListOf<ScannedFolder>()
        val scannedFiles = mutableListOf<ScannedFileItem>()

        for (uriString in folderUris) {
            val uri = Uri.parse(uriString)
            val root = DocumentFile.fromTreeUri(context, uri) ?: continue
            if (!root.exists() || !root.canRead()) continue

            val folderName = root.name ?: uri.lastPathSegment ?: "Selected Folder"
            var folderItemCount = 0
            var folderBytes = 0L

            val stack = ArrayDeque<Pair<DocumentFile, Int>>()
            stack.add(root to 0)

            while (stack.isNotEmpty()) {
                val (dir, depth) = stack.removeLast()
                if (depth > 6) continue // Guard against extreme recursion depth
                val children = try {
                    dir.listFiles()
                } catch (_: Exception) {
                    emptyArray()
                }
                for (child in children) {
                    val childName = child.name ?: continue
                    if (child.isDirectory) {
                        stack.add(child to depth + 1)
                    } else if (child.isFile) {
                        val size = child.length().coerceAtLeast(0L)
                        val mime = child.type.orEmpty()
                        val category = classifyFile(childName, mime, size)
                        scannedFiles.add(
                            ScannedFileItem(
                                uriString = child.uri.toString(),
                                folderUriString = uriString,
                                name = childName,
                                sizeBytes = size,
                                lastModifiedMs = child.lastModified(),
                                mimeType = mime,
                                category = category
                            )
                        )
                        folderItemCount++
                        folderBytes += size
                        if (scannedFiles.size % 20 == 0) {
                            onProgress(folderName, scannedFiles.size)
                        }
                    }
                }
            }

            scannedFolders.add(
                ScannedFolder(
                    uriString = uriString,
                    displayName = folderName,
                    itemCount = folderItemCount,
                    totalBytes = folderBytes
                )
            )
            onProgress(folderName, scannedFiles.size)
        }

        scannedFiles.sortByDescending { it.sizeBytes }
        FolderScanResult(folders = scannedFolders, files = scannedFiles)
    }

    suspend fun deleteFiles(
        itemsToDelete: List<ScannedFileItem>
    ): DeleteFilesResult = withContext(Dispatchers.IO) {
        var deletedCount = 0
        var freedBytes = 0L
        var failedCount = 0

        for (item in itemsToDelete) {
            val uri = Uri.parse(item.uriString)
            val doc = DocumentFile.fromSingleUri(context, uri)
            val success = try {
                doc?.delete() == true
            } catch (_: Exception) {
                false
            }
            if (success) {
                deletedCount++
                freedBytes += item.sizeBytes
            } else {
                failedCount++
            }
        }
        DeleteFilesResult(
            deletedCount = deletedCount,
            freedBytes = freedBytes,
            failedCount = failedCount
        )
    }

    private fun classifyFile(name: String, mime: String, sizeBytes: Long): StorageCategory {
        val lowerName = name.lowercase(Locale.US)
        val lowerMime = mime.lowercase(Locale.US)
        val largeThreshold = 25L * 1024L * 1024L

        if (sizeBytes >= largeThreshold) {
            return StorageCategory.LARGE
        }
        if (lowerMime.startsWith("video/") ||
            lowerMime.startsWith("image/") ||
            lowerMime.startsWith("audio/") ||
            lowerName.endsWith(".mp4") ||
            lowerName.endsWith(".mkv") ||
            lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg") ||
            lowerName.endsWith(".png") ||
            lowerName.endsWith(".mp3")
        ) {
            return StorageCategory.MEDIA
        }
        if (lowerName.endsWith(".zip") ||
            lowerName.endsWith(".rar") ||
            lowerName.endsWith(".7z") ||
            lowerName.endsWith(".apk") ||
            lowerName.endsWith(".log") ||
            lowerName.endsWith(".tmp") ||
            lowerName.endsWith(".cache") ||
            lowerName.endsWith(".bak")
        ) {
            return StorageCategory.ARCHIVES_TEMP
        }
        return StorageCategory.ALL
    }
}
