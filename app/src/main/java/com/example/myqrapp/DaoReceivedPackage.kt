package com.example.myqrapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoReceivedPackage {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPck(receivedPackage: ReceivedPackage)

    @Delete
    suspend fun deletePck(receivedPackage: ReceivedPackage)

    @androidx.room.Query("SELECT * FROM ReceivedPackage ORDER BY pckId ASC")
    fun getPckDataBySEQnr() : Flow<List<ReceivedPackage>>

    @androidx.room.Query("SELECT * FROM ReceivedPackage ORDER BY length ASC")
    fun getPckDataByLength() : Flow<List<ReceivedPackage>>

    @Query("DELETE FROM receivedPackage")
    suspend fun deleteAll()
}