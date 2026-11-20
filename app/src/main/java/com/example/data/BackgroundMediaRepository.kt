package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BackgroundMediaRepository(private val context: Context) {

    private val backgroundFile: File
        get() = File(context.filesDir, "custom_background.jpg")

    suspend fun importCustomBackgroundPhoto(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val maxDim = 1920
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > maxDim || bounds.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val outFile = backgroundFile
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
            }
            bitmap.recycle()
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCustomBackgroundPhoto() = withContext(Dispatchers.IO) {
        runCatching {
            if (backgroundFile.exists()) {
                backgroundFile.delete()
            }
        }
        Unit
    }
}
