package com.example.myqrapp
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BytePackageData(
    @PrimaryKey(autoGenerate = true)
    val pckId: Int = 0,
    val length: Int,
    var byteArray: ByteArray
)