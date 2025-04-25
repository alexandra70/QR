package com.example.myqrapp

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
data class ReceivedPackage(
    @PrimaryKey(autoGenerate = true)
    val pckId: Int = 0,
    val crc: Long,
    val length: Int,
    var content: String
)
