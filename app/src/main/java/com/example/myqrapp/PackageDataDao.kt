package com.example.myqrapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPck(packageData: PackageData)
    @Delete
    suspend fun deletePck(packageData: PackageData)

    @Query("SELECT * FROM packageData ORDER BY pckId ASC")
    fun getPckDataBySEQnr() : Flow<List<PackageData>>

    @Query("SELECT * FROM packageData ORDER BY length ASC")
    fun getPckDataByLength() : Flow<List<PackageData>>

    @Query("DELETE FROM packageData")
    suspend fun deleteAll()
}