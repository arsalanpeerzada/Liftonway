package dev.liftonway.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copies a photo the user picked into app storage, because the photo picker's access ends with the process.
 * Returns a file URI string that stays valid.
 */
@Singleton
class PhotoStore @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun keep(picked: Uri): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(picked)?.use { input -> file.outputStream().use { input.copyTo(it) } }
        Uri.fromFile(file).toString()
    }
}
