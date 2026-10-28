package com.memorystars.leitner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "📚",
    val color: Long = 0xFF7C4DFF,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cards",
    indices = [Index("deckId"), Index("dueAt")],
    foreignKeys = [ForeignKey(
        entity = DeckEntity::class, parentColumns = ["id"],
        childColumns = ["deckId"], onDelete = ForeignKey.CASCADE
    )]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val frontText: String = "",
    val backText: String = "",
    val frontImage: String? = null,
    val backImage: String? = null,
    val box: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val dueAt: Long = System.currentTimeMillis(),
    val correct: Int = 0,
    val wrong: Int = 0
)

@Entity(tableName = "review_logs", indices = [Index("at")])
data class ReviewLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val deckId: Long,
    val at: Long,
    val correct: Boolean,
    val xp: Int,
    val retry: Boolean = false
)
