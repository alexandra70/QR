package com.example.myqrapp;

import android.content.Context
import androidx.room.Database;
import androidx.room.Room
import androidx.room.RoomDatabase;
@Database(
        entities = [BytePackageData::class],
        version = 1
    )
abstract class BytePackageDataDB: RoomDatabase() {
    abstract val dao: BytePackageDataDao

    companion object {
        @Volatile
        private var INSTANCE: BytePackageDataDB? = null

        fun getDatabase(context: Context): BytePackageDataDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BytePackageDataDB::class.java,
                    "byte_data_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
