package com.example.myqrapp;

import android.content.Context
import androidx.room.Database;
import androidx.room.Room
import androidx.room.RoomDatabase;

@Database(
        entities = [PackageData::class],
        version = 1
)
abstract class PackageDataDB: RoomDatabase() {
    abstract val dao: PackageDataDao

    companion object {
        @Volatile
        private var INSTANCE: PackageDataDB? = null

        fun getDatabase(context: Context): PackageDataDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PackageDataDB::class.java,
                    "data_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
