package com.example.myqrapp;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = [ReceivedPackage::class],
    version = 1
)
abstract class PackageReceivedDB: RoomDatabase() {
    abstract val DAO_RECEIVED_PACKAGE: DaoReceivedPackage

    companion object {
        @Volatile
        private var INSTANCE: PackageReceivedDB? = null

        fun getDatabase(context: Context): PackageReceivedDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PackageReceivedDB::class.java,
                    "DATA_RECEIVED_DATABASE"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
