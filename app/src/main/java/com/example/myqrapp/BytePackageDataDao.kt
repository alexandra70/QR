package com.example.myqrapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface BytePackageDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPck(bytePackageData : BytePackageData)
    @Delete
    suspend fun deletePck(bytePackageData : BytePackageData)

    @Query("SELECT * FROM bytePackageData  ORDER BY pckId ASC")
    fun getPckDataBySEQnr() : Flow<List<BytePackageData>>

    @Query("SELECT * FROM bytePackageData  ORDER BY length ASC")
    fun getPckDataByLength() : Flow<List<BytePackageData>>

    @Query("DELETE FROM bytePackageData")
    suspend fun deleteAll()
}