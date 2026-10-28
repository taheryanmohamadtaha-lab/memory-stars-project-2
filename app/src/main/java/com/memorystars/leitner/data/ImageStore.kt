package com.memorystars.leitner.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

/** تصاویر در حافظه خصوصی برنامه ذخیره می‌شوند؛ در پایگاه داده فقط نام فایل نگه‌داری می‌شود. */
class ImageStore(private val ctx: Context) {
    val dir: File = File(ctx.filesDir, "images").apply { mkdirs() }

    fun file(name: String) = File(dir, name)
    fun newFile(): File = File(dir, "img_${UUID.randomUUID()}.jpg")

    fun importUri(uri: Uri): String? {
        return try {
            val f = newFile()
            val input = ctx.contentResolver.openInputStream(uri) ?: return null
            input.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
            f.name
        } catch (e: Exception) {
            null
        }
    }

    fun copyOf(name: String?): String? {
        if (name == null) return null
        val src = file(name)
        if (!src.exists()) return null
        val dst = newFile()
        src.copyTo(dst, overwrite = true)
        return dst.name
    }

    fun delete(name: String?) { if (name != null) file(name).delete() }

    fun bitmap(name: String, maxPx: Int): Bitmap? {
        val f = file(name)
        if (!f.exists()) return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= maxPx || o.outHeight / (s * 2) >= maxPx) s *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
    }
}
