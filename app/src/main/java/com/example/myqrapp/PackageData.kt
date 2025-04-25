package com.example.myqrapp

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
data class PackageData(
    @PrimaryKey(autoGenerate = true)
    val pckId: Int = 0,
    val crc: Long,
    val length: Int,
    var content: String
)
{
    // metoda deserializare(string)
    // return PackageData
    companion object {
        fun deserializePck(data: String): PackageData {

            val indID = data.indexOf("ID:") + 3
            val indIDEnd = data.indexOf("CRC:")

            val indCRC = data.indexOf("CRC:") + 4
            val indCRCEnd = data.indexOf("Length:")

            val indLen = data.indexOf("Length:") + 7
            val indLenEnd = data.indexOf("Payload:")

            val indPayload = data.indexOf("Payload:") + 8
            val payloadLen = data.substring(indLen, indLenEnd).toInt()

            //println("l" + data.substring(indID, indIDEnd).toInt())
            //println("l" + data.substring(indCRC, indCRCEnd).toLong())
            //println("l" + data.substring(indLen, indLenEnd))

            return PackageData(
                data.substring(indID, indIDEnd).toInt(),
                data.substring(indCRC, indCRCEnd).toLong(),
                payloadLen,
                data.substring(indPayload, indPayload + payloadLen)
            )
        }

        // metoda serializare()
        // return string;
        fun serializePck(pck: PackageData): String {
            val stringBuilder = StringBuilder()
            stringBuilder.append("ID:")
            stringBuilder.append(pck.pckId.toString())
            stringBuilder.append("CRC:")
            stringBuilder.append(pck.crc.toString())
            stringBuilder.append("Length:")
            stringBuilder.append(pck.length.toString())
            stringBuilder.append("Payload:")
            stringBuilder.append(pck.content)

            return stringBuilder.toString()
        }
    }
}