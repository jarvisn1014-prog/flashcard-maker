package com.nish.flashcards

import android.app.Application
import com.nish.flashcards.data.local.FlashcardDatabase

class FlashcardApp : Application() {
    lateinit var database: FlashcardDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = FlashcardDatabase.getInstance(this)
    }

    companion object {
        lateinit var instance: FlashcardApp
            private set
    }
}