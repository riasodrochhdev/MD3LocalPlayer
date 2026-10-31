package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackEntity::class], version = 2, exportSchema = false)
abstract class MediatekaDatabase : RoomDatabase() {
  abstract fun trackDao(): TrackDao

  companion object {
    @Volatile
    private var INSTANCE: MediatekaDatabase? = null

    fun getDatabase(context: Context): MediatekaDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          MediatekaDatabase::class.java,
          "mediateka_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
