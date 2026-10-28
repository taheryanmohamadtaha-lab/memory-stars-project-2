package com.memorystars.leitner.ui

import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.MutablePreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.memorystars.leitner.data.*
import com.memorystars.leitner.domain.Leitner
import com.memorystars.leitner.domain.Rewards
import com.memorystars.leitner.domain.Stats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SessionState(
    val current: CardEntity?, val remaining: Int, val total: Int, val done: Int,
    val correct: Int, val practice: Int, val xp: Int, val deckName: String,
    val step: Int = 0, val finished: Boolean = false, val goalReached: Boolean = false,
    val feedback: String = ""
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDb.build(app)
    private val dao = db.dao()
    val store = SettingsStore(app)
    val images = ImageStore(app)
    private val backup = BackupManager(app, db, images, store)

    val settings: StateFlow<AppSettings?> = store.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val decks: StateFlow<List<DeckEntity>> = dao.decks().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val cards: StateFlow<List<CardEntity>> = dao.cards().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val logs: StateFlow<List<ReviewLog>> = dao.logs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stats: StateFlow<Stats> = combine(decks, cards, logs, store.flow) { d, c, l, s ->
        Rewards.compute(d, c, l, s, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Stats())

    fun edit(b: (MutablePreferences) -> Unit) { viewModelScope.launch { store.edit(b) } }

    // ---------- دسته‌ها و کارت‌ها ----------
    fun saveDeck(existing: DeckEntity?, name: String, icon: String, color: Long) {
        viewModelScope.launch {
            if (existing == null) dao.insertDeck(DeckEntity(name = name, icon = icon, color = color))
            else dao.updateDeck(existing.copy(name = name, icon = icon, color = color))
        }
    }

    fun deleteDeck(d: DeckEntity) {
        viewModelScope.launch {
            dao.cardsOfDeck(d.id).forEach { images.delete(it.frontImage); images.delete(it.backImage) }
            dao.deleteDeck(d)
        }
    }

    suspend fun getCard(id: Long): CardEntity? = dao.card(id)

    fun saveCard(
        orig: CardEntity?, deckId: Long, front: String, back: String,
        fi: String?, bi: String?, done: () -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (orig == null) {
                dao.insertCard(CardEntity(deckId = deckId, frontText = front, backText = back,
                    frontImage = fi, backImage = bi, createdAt = now, dueAt = now))
            } else {
                dao.updateCard(orig.copy(deckId = deckId, frontText = front, backText = back, frontImage = fi, backImage = bi))
                if (orig.frontImage != null && orig.frontImage != fi) images.delete(orig.frontImage)
                if (orig.backImage != null && orig.backImage != bi) images.delete(orig.backImage)
            }
            done()
        }
    }

    fun deleteCard(c: CardEntity) {
        viewModelScope.launch {
            dao.deleteCard(c.id)
            images.delete(c.frontImage); images.delete(c.backImage)
        }
    }

    fun duplicateCard(c: CardEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.insertCard(CardEntity(deckId = c.deckId, frontText = c.frontText, backText = c.backText,
                frontImage = images.copyOf(c.frontImage), backImage = images.copyOf(c.backImage),
                createdAt = now, dueAt = now))
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.resetCards(now); dao.deleteAllLogs()
            store.edit { it[K.bonusXp] = 0; it[K.sessions] = 0; it[K.goalDay] = "" }
        }
    }

    // ---------- جلسه مرور ----------
    private val queue = ArrayDeque<CardEntity>()
    private val wrongIds = mutableSetOf<Long>()
    private var busy = false
    private var sTotal = 0; private var sDone = 0; private var sCorrect = 0
    private var sPractice = 0; private var sXp = 0; private var sStep = 0
    private var sDeckName = ""
    private val _session = MutableStateFlow<SessionState?>(null)
    val session = _session.asStateFlow()

    private val cheers = listOf("آفرین! داری بهتر می‌شی 🌟", "عالی بود! ادامه بده 🎉", "تلاش امروزت عالیه! 💪", "ستاره‌ی من! ⭐")
    private val retries = listOf("اشکال نداره، دوباره تمرین می‌کنیم 💛", "هر بار یاد می‌گیری، قوی‌تر می‌شی 🌱", "این کارت رو بعداً دوباره می‌بینیم 🙂")

    private fun publish(cur: CardEntity?, finished: Boolean = false, goal: Boolean = false, fb: String = "") {
        _session.value = SessionState(cur, queue.size, sTotal, sDone, sCorrect, sPractice, sXp, sDeckName, sStep, finished, goal, fb)
    }

    fun startSession(deckId: Long) {
        viewModelScope.launch {
            val due = dao.due(System.currentTimeMillis(), deckId)
            queue.clear(); wrongIds.clear(); queue.addAll(due)
            sTotal = due.size; sDone = 0; sCorrect = 0; sPractice = 0; sXp = 0; sStep = 0; busy = false
            sDeckName = if (deckId == 0L) "همه دسته‌ها" else decks.value.firstOrNull { it.id == deckId }?.name ?: ""
            publish(queue.firstOrNull())
        }
    }

    fun answer(correct: Boolean) {
        if (busy) return
        val c = queue.firstOrNull() ?: return
        busy = true
        viewModelScope.launch {
            try {
                queue.removeFirst()
                val st = settings.value ?: AppSettings()
                val now = System.currentTimeMillis()
                val retry = c.id in wrongIds
                var xp = 0
                var fb: String
                if (correct) {
                    if (retry) {
                        xp = Rewards.XP_TRY
                    } else {
                        val nb = Leitner.nextBoxCorrect(c.box)
                        dao.updateCard(c.copy(box = nb, lastReviewedAt = now,
                            dueAt = Leitner.nextDue(nb, now, st.intervals), correct = c.correct + 1))
                        xp = Rewards.XP_CORRECT
                        sCorrect++
                    }
                    sDone++
                    fb = cheers[sStep % cheers.size]
                } else {
                    if (!retry) {
                        dao.updateCard(c.copy(box = Leitner.nextBoxWrong(), lastReviewedAt = now,
                            dueAt = Leitner.nextDue(1, now, st.intervals), wrong = c.wrong + 1))
                        wrongIds.add(c.id)
                        sPractice++
                        xp = Rewards.XP_TRY
                        queue.add(minOf(3, queue.size), c) // بازگشت چند کارت بعد، فقط یک بار
                    } else {
                        sDone++
                    }
                    fb = retries[sStep % retries.size]
                }
                if (xp > 0) dao.insertLog(ReviewLog(cardId = c.id, deckId = c.deckId, at = now, correct = correct, xp = xp, retry = retry))
                sXp += xp
                sStep++
                if (queue.isEmpty()) {
                    val zone = ZoneId.systemDefault()
                    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                    val startToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
                    val reviewed = dao.reviewedSince(startToday)
                    val goalNew = reviewed >= st.goal && st.goalDay != today.toString()
                    val bonus = Rewards.XP_SESSION + if (goalNew) Rewards.XP_GOAL else 0
                    store.edit { p ->
                        p[K.sessions] = (p[K.sessions] ?: 0) + 1
                        p[K.bonusXp] = (p[K.bonusXp] ?: 0) + bonus
                        if (goalNew) p[K.goalDay] = today.toString()
                    }
                    sXp += bonus
                    publish(null, finished = true, goal = goalNew, fb = fb)
                } else publish(queue.first(), fb = fb)
            } finally { busy = false }
        }
    }

    fun endSession() { queue.clear(); _session.value = null }

    // ---------- پشتیبان ----------
    fun export(uri: Uri, result: (String) -> Unit) {
        viewModelScope.launch {
            result(try { backup.export(uri); "پشتیبان با موفقیت ساخته شد ✓" }
            catch (e: Exception) { "ساخت پشتیبان انجام نشد: ${e.message ?: "خطای نامشخص"}" })
        }
    }

    fun restore(uri: Uri, result: (String) -> Unit) {
        viewModelScope.launch {
            result(try { "بازیابی انجام شد؛ ${backup.restore(uri)} کارت بازگردانده شد ✓" }
            catch (e: BackupException) { e.message ?: "فایل معتبر نیست." }
            catch (e: Exception) { "بازیابی انجام نشد: ${e.message ?: "خطای نامشخص"}" })
        }
    }
}
