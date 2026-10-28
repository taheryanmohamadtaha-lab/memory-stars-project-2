package com.memorystars.leitner

import com.memorystars.leitner.data.CardEntity
import com.memorystars.leitner.domain.Practice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PracticeTest {
    private fun card(id: Long, f: String, b: String, box: Int = 1, wrong: Int = 0, deck: Long = 1) =
        CardEntity(id = id, deckId = deck, frontText = f, backText = b, box = box, wrong = wrong, createdAt = id)

    private val cards = listOf(
        card(1, "۲×۳", "۶"), card(2, "۳×۳", "۹", box = 4), card(3, "۴×۳", "۱۲", wrong = 2),
        card(4, "۵×۳", "۱۵", deck = 2), card(5, "۶×۳", "۱۸", box = 5)
    )

    @Test fun poolFilters() {
        assertEquals(5, Practice.pool(cards, 0, Practice.F_ALL).size)
        assertEquals(4, Practice.pool(cards, 1, Practice.F_ALL).size)
        assertEquals(listOf(2L, 5L), Practice.pool(cards, 0, Practice.F_LEARNED).map { it.id })
        assertEquals(listOf(3L), Practice.pool(cards, 0, Practice.F_HARD).map { it.id })
    }

    @Test fun orderings() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), Practice.order(cards.reversed(), Practice.ORDER_SEQ).map { it.id })
        assertEquals(3L, Practice.order(cards, Practice.ORDER_HARD).first().id)
        assertEquals(cards.map { it.id }.toSet(), Practice.order(cards, Practice.ORDER_RANDOM, Random(1)).map { it.id }.toSet())
    }

    @Test fun optionsContainTargetAndAreDistinct() {
        val o = Practice.options(cards[0], cards, cards, false, 4, Random(7))
        assertEquals(4, o.size)
        assertTrue(o.any { it.id == 1L })
        assertEquals(4, o.map { Practice.normalize(it.backText) }.toSet().size)
    }

    @Test fun optionsWithFewCards() {
        val two = cards.take(2)
        assertEquals(2, Practice.options(two[0], two, two, false).size)
    }

    @Test fun reverseUsesFrontAsAnswer() {
        assertEquals("۲×۳", Practice.answerText(cards[0], true))
        assertEquals("۶", Practice.questionText(cards[0], true))
    }

    @Test fun typedAnswerNormalization() {
        assertTrue(Practice.isTypedCorrect(" 56 ", "۵۶"))
        assertTrue(Practice.isTypedCorrect("كتاب", "کتاب"))
        assertTrue(Practice.isTypedCorrect("Cat!", "cat"))
        assertFalse(Practice.isTypedCorrect("", ""))
        assertFalse(Practice.isTypedCorrect("dog", "cat"))
    }
}
