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
    suspend fun insertPck(packageData: PackageData) //ar trebui sa nu pot insera pacete cu acelasi id deci??

    //sortare pachete? poate fac o eliminare inainte? are cum sa citeasca mai mult vreodata? face compilatorul asta asa ceva ???
    @Delete
    suspend fun deletePck(packageData: PackageData)

    @Query("SELECT * FROM packageData ORDER BY pckId ASC")
    fun getPckDataBySEQnr() : Flow<List<PackageData>>

    @Query("SELECT * FROM packageData ORDER BY length ASC")
    fun getPckDataByLength() : Flow<List<PackageData>>

    @Query("DELETE FROM packageData")
    suspend fun deleteAll()
}