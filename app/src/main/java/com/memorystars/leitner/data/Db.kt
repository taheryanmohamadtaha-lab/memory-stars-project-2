package com.memorystars.leitner.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM decks ORDER BY createdAt") fun decks(): Flow<List<DeckEntity>>
    @Query("SELECT * FROM cards ORDER BY createdAt DESC") fun cards(): Flow<List<CardEntity>>
    @Query("SELECT * FROM review_logs") fun logs(): Flow<List<ReviewLog>>

    @Insert suspend fun insertDeck(d: DeckEntity): Long
    @Update suspend fun updateDeck(d: DeckEntity)
    @Delete suspend fun deleteDeck(d: DeckEntity)

    @Insert suspend fun insertCard(c: CardEntity): Long
    @Update suspend fun updateCard(c: CardEntity)
    @Query("DELETE FROM cards WHERE id = :id") suspend fun deleteCard(id: Long)
    @Query("SELECT * FROM cards WHERE id = :id") suspend fun card(id: Long): CardEntity?
    @Query("SELECT * FROM cards WHERE deckId = :deckId") suspend fun cardsOfDeck(deckId: Long): List<CardEntity>

    @Insert suspend fun insertLog(l: ReviewLog): Long

    @Query("SELECT * FROM cards WHERE dueAt <= :now AND (:deckId = 0 OR deckId = :deckId) ORDER BY dueAt, id")
    suspend fun due(now: Long, deckId: Long): List<CardEntity>

    @Query("SELECT COUNT(DISTINCT cardId) FROM review_logs WHERE at >= :from")
    suspend fun reviewedSince(from: Long): Int

    @Query("UPDATE cards SET box = 1, correct = 0, wrong = 0, lastReviewedAt = NULL, dueAt = :now")
    suspend fun resetCards(now: Long)
    @Query("DELETE FROM review_logs") suspend fun deleteAllLogs()
    @Query("DELETE FROM decks") suspend fun deleteAllDecks()

    @Query("SELECT * FROM decks") suspend fun allDecks(): List<DeckEntity>
    @Query("SELECT * FROM cards") suspend fun allCards(): List<CardEntity>
    @Query("SELECT * FROM review_logs") suspend fun allLogs(): List<ReviewLog>
    @Insert suspend fun insertDecks(l: List<DeckEntity>)
    @Insert suspend fun insertCards(l: List<CardEntity>)
    @Insert suspend fun insertLogs(l: List<ReviewLog>)
}

@Database(entities = [DeckEntity::class, CardEntity::class, ReviewLog::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun build(c: Context): AppDb =
            Room.databaseBuilder(c.applicationContext, AppDb::class.java, "memorystars.db").build()
    }
}
