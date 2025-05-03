package com.example.myqrapp

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.zip.CRC32

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

            try {
                // step1. Verific prezența campurilor in pachet
                if (!data.contains("ID:")) {
                    Log.d("Deserializer", "Eroare: Lipseste campul ID")
                    throw IllegalArgumentException("Pachet invalid - lipseste campul ID")
                }

                if (!data.contains("CRC:")) {
                    Log.d("Deserializer", "Eroare: Lipseste campul CRC")
                    throw IllegalArgumentException("Pachet invalid - lipseste campul CRC")
                }

                if (!data.contains("Length:")) {
                    Log.d("Deserializer", "Eroare: Lipseste campul Length")
                    throw IllegalArgumentException("Pachet invalid - lipseste campul Length")
                }

                if (!data.contains("Payload:")) {
                    Log.d("Deserializer", "Eroare: Lipseste campul Payload")
                    throw IllegalArgumentException("Pachet invalid - lipseste campul Payload")
                }

                // step 2. Extrag campurile si valorile asociate
                val indID = data.indexOf("ID:") + 3
                val indIDEnd = data.indexOf("CRC:")

                val indCRC = data.indexOf("CRC:") + 4
                val indCRCEnd = data.indexOf("Length:")

                val indLen = data.indexOf("Length:") + 7
                val indLenEnd = data.indexOf("Payload:")

                val indPayload = data.indexOf("Payload:") + 8
                val payloadLen = data.substring(indLen, indLenEnd).toInt()

                if (indID == -1 || indCRC == -1 || indLen == -1 || indPayload == -1) {
                    Log.d("[SAU LA ASTA ARE PROBL...]", "CAREMAI")
                    throw IllegalArgumentException("Pachet invalid - probleme la extragere campuri")
                }

                //println("l" + data.substring(indID, indIDEnd).toInt())
                //println("l" + data.substring(indCRC, indCRCEnd).toLong())
                //println("l" + data.substring(indLen, indLenEnd))

                // step 3 :trebuie sa extrag valorile efective ale campuilor
                val id = data.substring(indID, indIDEnd).toInt()
                val crc = data.substring(indCRC, indCRCEnd).toLong()
                val payload = data.substring(indPayload, indPayload + payloadLen)

                //step 5: compunere pachet
                return PackageData(
                    id,
                    crc,
                    payloadLen,
                    payload
                )

            } catch (e: Exception) {
                throw e
            }
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

        private fun calculateCRC(data: ByteArray): Long {
            val crc = CRC32()
            crc.update(data)
            return crc.value
        }
    }
}