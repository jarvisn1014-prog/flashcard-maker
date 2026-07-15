package com.nish.flashcards.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nish.flashcards.data.model.Deck
import com.nish.flashcards.data.model.Flashcard
import com.nish.flashcards.data.model.ReviewLog

@Database(
    entities = [Deck::class, Flashcard::class, ReviewLog::class],
    version = 1,
    exportSchema = false
)
abstract class FlashcardDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun reviewLogDao(): ReviewLogDao

    companion object {
        @Volatile
        private var INSTANCE: FlashcardDatabase? = null

        fun getInstance(context: Context): FlashcardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlashcardDatabase::class.java,
                    "flashcard_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}