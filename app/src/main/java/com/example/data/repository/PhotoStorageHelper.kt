package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PhotoStorageHelper {

    private const val DIRECTORY_NAME = "prayer_journal_photos"

    private fun getJournalDir(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveBitmapLocally(context: Context, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val dir = getJournalDir(context)
        val filename = "prayer_${UUID.randomUUID()}.jpg"
        val file = File(dir, filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    }

    suspend fun saveUriLocally(context: Context, sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val dir = getJournalDir(context)
        val filename = "prayer_${UUID.randomUUID()}.jpg"
        val file = File(dir, filename)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    }

    suspend fun deleteLocalPhoto(path: String?) = withContext(Dispatchers.IO) {
        if (!path.isNullOrBlank()) {
            try {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            } catch (_: Exception) { }
        }
    }
}
