package com.memorystars.leitner.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupException(msg: String) : Exception(msg)

class BackupManager(
    private val ctx: Context,
    private val db: AppDb,
    private val images: ImageStore,
    private val store: SettingsStore
) {
    private val dao = db.dao()

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        val s = store.flow.first()
        val root = JSONObject().put("app", "memorystars").put("version", 1)
        root.put("settings", JSONObject()
            .put("name", s.name).put("goal", s.goal).put("bonusXp", s.bonusXp)
            .put("sessions", s.sessions).put("goalDay", s.goalDay)
            .put("intervals", s.intervals.joinToString(",")))
        val decks = JSONArray()
        dao.allDecks().forEach {
            decks.put(JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon)
                .put("color", it.color).put("createdAt", it.createdAt))
        }
        val cards = dao.allCards()
        val ca = JSONArray()
        cards.forEach {
            ca.put(JSONObject().put("id", it.id).put("deckId", it.deckId)
                .put("ft", it.frontText).put("bt", it.backText)
                .put("fi", it.frontImage ?: JSONObject.NULL).put("bi", it.backImage ?: JSONObject.NULL)
                .put("box", it.box).put("createdAt", it.createdAt)
                .put("last", it.lastReviewedAt ?: JSONObject.NULL).put("dueAt", it.dueAt)
                .put("correct", it.correct).put("wrong", it.wrong))
        }
        val la = JSONArray()
        dao.allLogs().forEach {
            la.put(JSONObject().put("id", it.id).put("cardId", it.cardId).put("deckId", it.deckId)
                .put("at", it.at).put("correct", it.correct).put("xp", it.xp).put("retry", it.retry))
        }
        root.put("decks", decks).put("cards", ca).put("logs", la)
        val out = ctx.contentResolver.openOutputStream(uri) ?: throw BackupException("امکان نوشتن فایل نیست.")
        out.use { o ->
            ZipOutputStream(o).use { z ->
                z.putNextEntry(ZipEntry("data.json"))
                z.write(root.toString().toByteArray(Charsets.UTF_8))
                z.closeEntry()
                cards.flatMap { listOfNotNull(it.frontImage, it.backImage) }.toSet().forEach { n ->
                    val f = images.file(n)
                    if (f.exists()) {
                        z.putNextEntry(ZipEntry("images/$n"))
                        f.inputStream().use { it.copyTo(z) }
                        z.closeEntry()
                    }
                }
            }
        }
    }

    /** پس از اعتبارسنجی کامل، داده‌های فعلی را جایگزین می‌کند. تعداد کارت‌های بازیابی‌شده را برمی‌گرداند. */
    suspend fun restore(uri: Uri): Int = withContext(Dispatchers.IO) {
        val tmp = File(ctx.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var json: String? = null
        val input = ctx.contentResolver.openInputStream(uri) ?: throw BackupException("امکان خواندن فایل نیست.")
        try {
            input.use { i ->
                ZipInputStream(i).use { z ->
                    var e = z.nextEntry
                    while (e != null) {
                        val n = e.name
                        if (!e.isDirectory) {
                            if (n == "data.json") json = z.readBytes().toString(Charsets.UTF_8)
                            else if (n.startsWith("images/")) {
                                val base = n.removePrefix("images/")
                                if (base.isNotEmpty() && !base.contains("/") && !base.contains("\\") && !base.contains("..")) {
                                    File(tmp, base).outputStream().use { o -> z.copyTo(o) }
                                }
                            }
                        }
                        z.closeEntry()
                        e = z.nextEntry
                    }
                }
            }
        } catch (e: BackupException) { throw e } catch (e: Exception) {
            throw BackupException("فایل پشتیبان معتبر نیست یا خراب است.")
        }
        val data = json ?: throw BackupException("این فایل، پشتیبان ستاره‌های حافظه نیست.")
        val decks: List<DeckEntity>
        val cards: List<CardEntity>
        val logs: List<ReviewLog>
        val st: JSONObject
        try {
            val root = JSONObject(data)
            if (root.optString("app") != "memorystars" || root.optInt("version") != 1)
                throw BackupException("نسخه یا نوع فایل پشتیبان پشتیبانی نمی‌شود.")
            st = root.getJSONObject("settings")
            decks = root.getJSONArray("decks").let { a ->
                (0 until a.length()).map {
                    val o = a.getJSONObject(it)
                    DeckEntity(o.getLong("id"), o.getString("name"), o.getString("icon"), o.getLong("color"), o.getLong("createdAt"))
                }
            }
            val ids = decks.map { it.id }.toSet()
            fun img(o: JSONObject, k: String): String? =
                if (o.isNull(k)) null else o.getString(k).takeIf { File(tmp, it).exists() }
            cards = root.getJSONArray("cards").let { a ->
                (0 until a.length()).map {
                    val o = a.getJSONObject(it)
                    val d = o.getLong("deckId")
                    if (d !in ids) throw BackupException("فایل پشتیبان ناسازگار است.")
                    CardEntity(
                        o.getLong("id"), d, o.getString("ft"), o.getString("bt"),
                        img(o, "fi"), img(o, "bi"), o.getInt("box").coerceIn(1, 5),
                        o.getLong("createdAt"), if (o.isNull("last")) null else o.getLong("last"),
                        o.getLong("dueAt"), o.getInt("correct"), o.getInt("wrong")
                    )
                }
            }
            logs = root.getJSONArray("logs").let { a ->
                (0 until a.length()).map {
                    val o = a.getJSONObject(it)
                    ReviewLog(o.getLong("id"), o.getLong("cardId"), o.getLong("deckId"), o.getLong("at"),
                        o.getBoolean("correct"), o.getInt("xp"), o.getBoolean("retry"))
                }
            }
        } catch (e: BackupException) { throw e } catch (e: Exception) {
            throw BackupException("محتوای فایل پشتیبان معتبر نیست.")
        }
        db.withTransaction {
            dao.deleteAllDecks(); dao.deleteAllLogs()
            dao.insertDecks(decks); dao.insertCards(cards); dao.insertLogs(logs)
        }
        images.dir.listFiles()?.forEach { it.delete() }
        tmp.listFiles()?.forEach { it.copyTo(images.file(it.name), overwrite = true) }
        tmp.deleteRecursively()
        store.edit { p ->
            p[K.name] = st.optString("name", "")
            p[K.goal] = st.optInt("goal", 10)
            p[K.bonusXp] = st.optInt("bonusXp", 0)
            p[K.sessions] = st.optInt("sessions", 0)
            p[K.goalDay] = st.optString("goalDay", "")
            p[K.intervals] = st.optString("intervals", "")
        }
        cards.size
    }
}
