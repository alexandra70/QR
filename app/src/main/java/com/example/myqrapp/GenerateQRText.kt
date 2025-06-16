package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.WindowManager
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
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.util.zip.CRC32
import kotlin.math.max
import kotlin.math.min

open class GenerateQRText : AppCompatActivity() {

    private var nrPck = 0

    lateinit var qrIV: ImageView
    lateinit var msgEdt: EditText
    lateinit var generateQRBtn: Button

    lateinit var database: BytePackageDataDB
    lateinit var dao: BytePackageDataDao

    private val ackChannel = Channel<Int>(Channel.UNLIMITED)
    private val sizeChannel = Channel<Int>(Channel.UNLIMITED)
    private var ackCounter: Int = 0

    private var packageNumber: Int = 1
    private val maxSliceSize: Int = SenderReaderVars.payloadLength
    private var sliceSize: Int = maxSliceSize

    private var fileName: String = "insert_result.txt"

    private lateinit var serverSocket: ServerSocket
    private lateinit var client: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr_text)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "GENERATE QR TEXT"

        qrIV = findViewById(R.id.idIVQrcode)
        msgEdt = findViewById(R.id.idEdt)
        generateQRBtn = findViewById(R.id.ButonGenerareQREfectiva)

        database = BytePackageDataDB.getDatabase(applicationContext)
        dao = database.dao

        startAckServer()

        generateQRBtn.setOnClickListener {

            if(!msgEdt.text.toString().isNullOrEmpty()) {

                lifecycleScope.launch {
                    deleteAllDatabaseEntriesByteArray(this@GenerateQRText)
                    //constructie pachete
                    val success = stringBreak(msgEdt.text.toString())
                    if (success) {
                        Log.i(ContentValues.TAG, "String correctly processed")
                    } else {
                        Log.i(ContentValues.TAG, "Failed to process String correctly")
                    }

                    //               ACK PCK 0                 //
                    processFirstPck()
                    val firstAck = ackChannel.receive()
                    if (firstAck != 0) {
                        Log.e("SYNC", "ACK invalid pentru primul pachet ($firstAck), opresc transmisia")
                        return@launch
                    }
                    Log.d("SYNC", "ACK primit pentru pachetul 0, începem transmisia cadrului principal")


                    //       ACK SEQ                           //
                    val packageDataList = dao.getPckDataBySEQnr().first()
                    val iterator = packageDataList.iterator()

                    var iteratii = 0

                    while (iterator.hasNext()) {

                        iteratii++
                        Log.d("nr inter", iteratii.toString())

                        val packageData = iterator.next()
                        var sent = 0
                        val content = packageData.byteArray

                        while (sent < content.size){
                            //packageData.pckId = packageNumber
                            val bytesToSend = content.copyOfRange(sent,
                                min(content.size,sent + sliceSize)
                            )
                            val dataToSend = Base64.encodeToString(bytesToSend, Base64.NO_WRAP)
                            val pckToSend = PackageData(packageNumber, computeCRC(dataToSend), dataToSend.length, dataToSend)

                            Log.d(ContentValues.TAG, "Processing content: $dataToSend")
                            localQRGenerate(pckToSend)
                            // delta = time - time0  = > delay(SenderReaderVars.packetDelayMs - (timeSource.markNow() - mark).inWholeMilliseconds) // - delta

                            // wait ACK pentru pachetul primit
                            val ackId = ackChannel.receive()
                            Log.d("POINT", "($packageNumber) (${dataToSend.length}))")
                            if(ackId == packageNumber){
                                val lengthRead = sizeChannel.receive()
                                ackCounter += 1
                                if(ackCounter >= 4){
                                    sliceSize = min(maxSliceSize, sliceSize * SenderReaderVars.SLICING_FACTOR)
                                    ackCounter = 0
                                }
                                Log.d("YEP", "($packageNumber) citit corect ($ackCounter)")
                                packageNumber += 1
                                sent += lengthRead
                            }
                            else if(ackId == -2){
                                ackCounter = 0
                                sliceSize = max(sliceSize / SenderReaderVars.SLICING_FACTOR, SenderReaderVars.MIN_SLICE_SIZE)
                                Log.d("NOPE", "cerere de micsorare")
                            }
                            else if (ackId != packageData.pckId) {
                                Log.e("SYNC", "Pachetul primit ($ackId) nu corespunde cu cel trimis (${packageData.pckId})")
                                //break
                            }
                        }

                    }

                    localQRGenerateLastFrame("END")
                    while(ackChannel.receive() != -1){}
                    Log.d("END_ACK_CODE_RECEIVED", "bla")
                    /* --------------------------------------------------- */

                    qrIV.setImageBitmap(null)

                    onDestroy()

                }
            }
        }
    }

    private suspend fun stringBreak(textInserted: String): Boolean {


        var chunkSize = SenderReaderVars.payloadLength

        if (textInserted.isEmpty()) return false

        //lista de stringuri(==un singur pachet sau mai multe)
        val chunks = if (textInserted.length <= chunkSize) {
            listOf(textInserted)
        } else {
            textInserted.chunked(chunkSize)
        }

        withContext(Dispatchers.IO) {
            for ((index, chunk) in chunks.withIndex()) {
                val chunkBytes: ByteArray = chunk.toByteArray(Charsets.UTF_8)

                val base64Payload = Base64.encodeToString(chunkBytes, Base64.NO_WRAP)
                val crc = computeCRC(base64Payload)

                database.dao.insertPck(
                    BytePackageData(
                        index + 1,
                        chunk.length,
                        chunkBytes
                    )
                )
                Log.d("ce adaug in db",index.toString() + " " + chunk.length + " " + "caut ce adaug in db   ")
                nrPck++
            }
        }
        return true
    }

    private fun startAckServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(SenderReaderVars.PORT)
                client = serverSocket.accept()
                while (true) {
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val ack = reader.readLine()
                    Log.d("ACK_SERVER", "Primit: $ack")

                    if (ack.startsWith("ACK:")) {
                        val parts = ack.split(":")
                        if (parts.size >= 3) {
                            val id = parts[1].toIntOrNull()
                            val length = parts[2].toIntOrNull()
                            Log.d("ACK_RECEIVED", "Pachet $id cu lungime $length")

                            if (id == -1){
                                withContext(Dispatchers.Main) {
                                    this@GenerateQRText.finish()
                                }
                                Log.d("CLOSE_CONN", "All database entries deleted")
                                break
                            }

                            if (id != null) {
                                ackChannel.send(id)
                                if(id > 0 && length != null){
                                    sizeChannel.send(length)
                                }
                            }

                        } else {
                            Log.e("ACK_SERVER", "Format invalid: $ack")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ACK LA SENDER", "Eroare server", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            client.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Log.d("CLOSE SOCKETS","Sockets closed in onDestroy (Fragment)...")
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun deleteAllDatabaseEntriesByteArray(context: Context) {
        val database = BytePackageDataDB.getDatabase(context)
        val dao = database.dao
        withContext(Dispatchers.IO) {
            dao.deleteAll()
        }
        Log.d(ContentValues.TAG, "All database entries deleted")
    }

    fun getLocalIpAddress(): String {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
    }
    private fun processFirstPck() {

        val stringBuilder = StringBuilder()

        stringBuilder.append("FileName:")
        stringBuilder.append(fileName)

        val ip = getLocalIpAddress()
        stringBuilder.append("IP:$ip")

        val port = SenderReaderVars.PORT
        stringBuilder.append("PORT:$port")

        //send data
        localQRGenerate(PackageData(
            0,
            0,
            9 + fileName.toString().length
                    + 3 + ip.toString().length
                    + 5 + port.toString().length,
            stringBuilder.toString()
        ))
    }
    private suspend fun readFileContent(context: Context, uri: Uri?): Boolean {
        val database = BytePackageDataDB.getDatabase(context)

        var chunkSize =
            SenderReaderVars.payloadLength // - SenderReaderVars.exceptPayload //chr per pachet(bytes?)

        if (uri != null) {
            return try {
                val inputStream =
                    withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri) }
                var bytesRead: Int
                val buffer = ByteArray(chunkSize)
                var i = 0
                inputStream?.use { stream ->
                    while (withContext(Dispatchers.IO) {
                            stream.read(buffer).also { bytesRead = it }
                        } != -1) {
                        val chunk = buffer.copyOfRange(0, bytesRead)

                        //val chunkString = String(chunk, Charset.forName("ISO-8859-1"))
                        val chunkString = Base64.encodeToString(chunk, Base64.NO_WRAP)

                        Log.d("chunk", chunk.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) })

                        Log.d("chunkString",  chunkString)

                        // Base64.encodeToString(chunk, 0)
                        //stringBuilder.append(chunkString)
                        //sa vad daca e mai bine asa ? val payload = String(chunk, Charsets.ISO_8859_1)
                        val crc = computeCRC(chunkString)
                        i++
                        withContext(Dispatchers.IO) {
                            database.dao.insertPck(
                                BytePackageData(
                                    i,
                                    chunk.size,
                                    chunk
                                )
                            )
                        }

                        nrPck++;
                    }
                }
                //Log.d(TAG, "Conținutul fișierului: $stringBuilder")
                true
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Eroare la citirea fișierului", e)
                false
            }
        } else {
            return false
        }
    }

    fun localQRGenerateLastFrame(endString: String): Boolean {

        val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)

        val width = point.x
        val height = point.y
        var dimen = if (width < height) width else height

        dimen = (dimen * SenderReaderVars.qrSizeScaleFactor).toInt() //dimen = dimen * 3 / 4

        val bitmap = generateQRCode(
            endString,
            dimen,
            errorCorrectionLevel = ErrorCorrectionLevel.L
        ) //val qrEncoder = QRGEncoder(data, null, QRGContents.Type.TEXT, dimen)
        if (bitmap != null) {

            qrIV.setImageBitmap(bitmap)
            return true
        }
        return false
    }

    fun localQRGenerate(packageData: PackageData): Boolean {

        val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display = windowManager.defaultDisplay
        val point = Point()
        display.getSize(point)

        val width = point.x
        val height = point.y
        var dimen = if (width < height) width else height

        dimen = (dimen * SenderReaderVars.qrSizeScaleFactor).toInt() //dimen = dimen * 3 / 4

        //pregatire date de codificat
        val data = PackageData.serializePck(packageData)
        Log.d("PROC_QR", "face qr")

        val bitmap = generateQRCode(
            data,
            dimen,
            errorCorrectionLevel = ErrorCorrectionLevel.L
        ) //val qrEncoder = QRGEncoder(data, null, QRGContents.Type.TEXT, dimen)
        if (bitmap != null) {
            Log.d("GEN_QR", "qr nenul")
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
                //EncodeHintType.CHARACTER_SET to "ISO-8859-1",
                EncodeHintType.CHARACTER_SET to "UTF-8",
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
}