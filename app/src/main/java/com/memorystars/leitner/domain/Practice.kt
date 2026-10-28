package com.memorystars.leitner.domain

import com.memorystars.leitner.data.CardEntity
import kotlin.random.Random

/** منطق خالص تمرین آزاد. تمرین روی جعبه‌های لایتنر و امتیاز اثری ندارد. */
object Practice {
    const val CLASSIC = 0
    const val CHOICE = 1
    const val TYPING = 2
    const val MATCH = 3

    const val ORDER_SEQ = 0
    const val ORDER_RANDOM = 1
    const val ORDER_HARD = 2

    const val F_ALL = 0
    const val F_WEAK = 1
    const val F_LEARNED = 2
    const val F_HARD = 3

    fun questionText(c: CardEntity, rev: Boolean): String = if (rev) c.backText else c.frontText
    fun answerText(c: CardEntity, rev: Boolean): String = if (rev) c.frontText else c.backText

    fun pool(cards: List<CardEntity>, deckId: Long, filter: Int): List<CardEntity> =
        cards.filter { c ->
            (deckId == 0L || c.deckId == deckId) && when (filter) {
                F_WEAK -> c.box <= 2
                F_LEARNED -> c.box >= 4
                F_HARD -> c.wrong > 0
                else -> true
            }
        }

    /** کارت‌هایی که برای حالت انتخاب‌شده قابل استفاده‌اند. */
    fun usable(pool: List<CardEntity>, mode: Int, rev: Boolean): List<CardEntity> =
        if (mode == CHOICE || mode == TYPING) pool.filter { answerText(it, rev).isNotBlank() } else pool

    fun order(list: List<CardEntity>, order: Int, rnd: Random = Random.Default): List<CardEntity> =
        when (order) {
            ORDER_RANDOM -> list.shuffled(rnd)
            ORDER_HARD -> list.sortedWith(
                compareByDescending<CardEntity> { it.wrong }.thenBy { it.box }.thenBy { it.id }
            )
            else -> list.sortedWith(compareBy<CardEntity>({ it.createdAt }, { it.id }))
        }

    /** گزینه‌های چندگزینه‌ای: کارت هدف + حداکثر n-1 گزینه‌ی نادرست با پاسخ متفاوت، به ترتیب تصادفی. */
    fun options(
        target: CardEntity, pool: List<CardEntity>, all: List<CardEntity>,
        rev: Boolean, n: Int = 4, rnd: Random = Random.Default
    ): List<CardEntity> {
        val seen = mutableSetOf(normalize(answerText(target, rev)))
        val picked = mutableListOf<CardEntity>()
        for (src in listOf(pool.shuffled(rnd), all.shuffled(rnd))) {
            for (c in src) {
                if (picked.size >= n - 1) break
                val t = normalize(answerText(c, rev))
                if (t.isNotEmpty() && seen.add(t)) picked.add(c)
            }
        }
        return (picked + target).shuffled(rnd)
    }

    /** یکسان‌سازی متن برای مقایسه‌ی پاسخ تایپ‌شده (فاصله، نشانه‌گذاری، ی/ک عربی، ارقام فارسی). */
    fun normalize(s: String): String {
        val sb = StringBuilder()
        for (ch in s.trim().lowercase()) {
            val c = when (ch) {
                'ي' -> 'ی'
                'ك' -> 'ک'
                in '۰'..'۹' -> '0' + (ch - '۰')
                in '٠'..'٩' -> '0' + (ch - '٠')
                else -> ch
            }
            if (c.isLetterOrDigit()) sb.append(c)
        }
        return sb.toString()
    }

    fun isTypedCorrect(input: String, answer: String): Boolean {
        val a = normalize(answer)
        return a.isNotEmpty() && normalize(input) == a
    }
}
