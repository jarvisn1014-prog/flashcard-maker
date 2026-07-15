package com.nish.flashcards.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.nish.flashcards.data.model.Deck
import com.nish.flashcards.data.model.Flashcard
import com.nish.flashcards.data.model.ReviewLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY lastStudiedAt DESC, createdAt DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: String): Deck?

    @Query("SELECT * FROM decks WHERE name LIKE '%' || :query || '%'")
    fun searchDecks(query: String): Flow<List<Deck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Update
    suspend fun updateDeck(deck: Deck)

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("UPDATE decks SET cardCount = :count WHERE id = :deckId")
    suspend fun updateCardCount(deckId: String, count: Int)

    @Query("UPDATE decks SET lastStudiedAt = :timestamp WHERE id = :deckId")
    suspend fun updateLastStudied(deckId: String, timestamp: Long)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id")
    fun getCardsForDeck(deckId: String): Flow<List<Flashcard>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY nextReviewDate ASC")
    suspend fun getCardsForDeckSync(deckId: String): List<Flashcard>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND nextReviewDate <= :now ORDER BY nextReviewDate ASC")
    suspend fun getDueCards(deckId: String, now: Long): List<Flashcard>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    suspend fun getCardCount(deckId: String): Int

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND nextReviewDate <= :now")
    suspend fun getDueCount(deckId: String, now: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Flashcard>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Flashcard): Long

    @Update
    suspend fun updateCard(card: Flashcard)

    @Delete
    suspend fun deleteCard(card: Flashcard)

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteAllCardsInDeck(deckId: String)
}

@Dao
interface ReviewLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReviewLog)

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewedAt DESC")
    suspend fun getLogsForCard(cardId: String): List<ReviewLog>
}