package com.example.myqrapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.zip.CRC32
import kotlin.time.TimeSource

open class GenerateQRText : AppCompatActivity() {

    private var nrPck = 0

    lateinit var qrIV: ImageView
    lateinit var msgEdt: EditText
    lateinit var generateQRBtn: Button

    lateinit var database: PackageDataDB
    lateinit var dao: PackageDataDao

    private val ackChannel = Channel<Int>(Channel.UNLIMITED)
    private var serverStarted = false
    private var serverSocket: ServerSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr_text)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "GENERATE QR TEXT"

        qrIV = findViewById(R.id.idIVQrcode)
        msgEdt = findViewById(R.id.idEdt)
        generateQRBtn = findViewById(R.id.ButonGenerareQREfectiva)

        database = PackageDataDB.getDatabase(applicationContext)
        dao = database.dao

        startAckServer()

        generateQRBtn.setOnClickListener {

            if(!msgEdt.text.toString().isNullOrEmpty()) {

                lifecycleScope.launch {
                    nrPck = 0
                    deleteAllDatabaseEntries()
                    //constructie pachete
                    val success = stringBreak(msgEdt.text.toString())
                    if (success) {
                        Log.i(ContentValues.TAG, "String correctly processed")
                    } else {
                        Log.i(ContentValues.TAG, "Failed to process String correctly")
                    }

                    //trimit primul pachet de cateva ori - >
                    var timeStart = SenderReaderVars.initialSyncTimeMs // 7s? >> time

                    val startTimeTest = SystemClock.elapsedRealtime()
                    Log.d(
                        "cand incepe",
                        (startTimeTest).toString()
                    )


                    //               ACK PCK 0                 //
                    processFirstPck(timeStart)
                    val firstAck = ackChannel.receive()
                    if (firstAck != 0) {
                        Log.e("SYNC", "ACK invalid pentru primul pachet ($firstAck), opresc transmisia")
                        return@launch
                    }
                    Log.d("SYNC", "ACK primit pentru pachetul 0, începem transmisia cadrului principal")



                    val endTimeTest = SystemClock.elapsedRealtime()

                    Log.d(
                        "cand se terimna, dar diferenta",
                        (endTimeTest - startTimeTest).toString()
                    )

                    //       ACK SEQ                           //
                    val packageDataList = dao.getPckDataBySEQnr().first()
                    val iterator = packageDataList.iterator()
                    val timeSource = TimeSource.Monotonic

                    while (iterator.hasNext()) {
                        // time0 = time
                        val mark = timeSource.markNow()
                        val packageData = iterator.next()
                        val content = packageData.content
                        Log.d(ContentValues.TAG, "Processing content: $content")
                        localQRGenerate(packageData)
                        // delta = time - time0 => delay(SenderReaderVars.packetDelayMs - (timeSource.markNow() - mark).inWholeMilliseconds) // - delta

                        val ackId = ackChannel.receive()
                        if (ackId != packageData.pckId) {
                            Log.e("SYNC", "Pachetul primit ($ackId) nu corespunde cu cel trimis (${packageData.pckId})")
                            break
                        }
                    }
                    qrIV.setImageBitmap(null)
                    //textEncodeDataButtonPressed.text = stringBuilder.toString()
                }
            }
        }
    }

    private suspend fun stringBreak(textInserted: String): Boolean {

        var chunkSize =
            SenderReaderVars.payloadLengthForInsertedText - SenderReaderVars.exceptPayloadForInsertedText //chr per pachet(bytes?)

        /* todo */
        chunkSize = 1

        if (textInserted.isEmpty()) return false

        //lista de stringuri(==un singur pachet sau mai multe)
        val chunks = if (textInserted.length <= chunkSize) {
            listOf(textInserted)
        } else {
            textInserted.chunked(chunkSize)
        }

        withContext(Dispatchers.IO) {
            for ((index, chunk) in chunks.withIndex()) {
                val crc = computeCRC(chunk)

                database.dao.insertPck(
                    PackageData(
                        index + 1,
                        crc,
                        chunk.length,
                        chunk
                    ))
                Log.d("ce adaug in db",index.toString() + " " + chunk.length + " " + "caut ce adaug in db   ")
                nrPck++
            }
        }
        return true
    }

    fun getLocalIpAddress(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        //asta o sa mearga numai pt ipv4si numai in retele wifi
        return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }

    private fun startAckServer() {
        if (serverStarted) return
        serverStarted = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(SenderReaderVars.PORT)
                while (true) {
                    val client = serverSocket!!.accept()
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val ack = reader.readLine()
                    Log.d("ACK_SERVER", "Primit: $ack")
                    if (ack.startsWith("ACK:")) {
                        val id = ack.removePrefix("ACK:").toIntOrNull()
                        id?.let { ackChannel.send(it) }
                    }
                    client.close()
                }
            } catch (e: Exception) {
                Log.e("ACK LA SENDER", "Eroare server", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverSocket?.close()
    }
    private fun processFirstPck(time: Long) {

        val stringBuilder = StringBuilder()
        stringBuilder.append("Time:")
        stringBuilder.append(time.toString())

        stringBuilder.append("NrPck:")
        stringBuilder.append(nrPck.toString())

        stringBuilder.append("FramesTime:")
        stringBuilder.append(SenderReaderVars.packetDelayMs)

        val ip = getLocalIpAddress()
        stringBuilder.append("IP:$ip")

        //send data
        localQRGenerate(
            PackageData(
                0,
                0,
                5 + time.toString().length
                        + 6 + nrPck.toString().length
                        + 11 + SenderReaderVars.packetDelayMs.toString().length
                        + 3 + ip.toString().length,
                stringBuilder.toString()
            )
        )

        println("timpul de procesaer si cum arata payload : " + stringBuilder.toString() + " " + time + "cate pachete " + nrPck)
    }

    fun localQRGenerate(packageData: PackageData): Boolean {

        val display: Display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)

        val width = point.x
        val height = point.y
        var dimen = if (width < height) width else height

        dimen = (dimen * SenderReaderVars.qrSizeScaleFactor).toInt() //dimen = dimen * 3 / 4

        //pregatire date de codificat
        val data = PackageData.serializePck(packageData)

        val bitmap = generateQRCode(
            data,
            dimen,
            errorCorrectionLevel = ErrorCorrectionLevel.L
        ) //val qrEncoder = QRGEncoder(data, null, QRGContents.Type.TEXT, dimen)
        if (bitmap != null) {
            qrIV.setImageBitmap(bitmap)
            return true
        }
        return false
    }

    private fun computeCRC(text: String): Long {
        val crc = CRC32()
        crc.update(text.toByteArray(Charsets.UTF_8))
        return crc.value
    }

    private fun generateQRCode(
        data: String,
        size: Int,
        errorCorrectionLevel: ErrorCorrectionLevel
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF_8",
                EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
                EncodeHintType.MARGIN to 1 // o margine mai putin groasa in jurul codului
            )

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun deleteAllDatabaseEntries() {
        withContext(Dispatchers.IO) {
            dao.deleteAll()
        }
        Log.d(ContentValues.TAG, "All database entries deleted")
    }
}