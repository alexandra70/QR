package com.example.myqrapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
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
import java.util.zip.CRC32
import kotlin.time.TimeSource

class FilesSystem : Fragment() {

    private val stringBuilder = StringBuilder()
    private lateinit var filePicker: ActivityResultLauncher<Intent>
    private var processData: Boolean = false
    private var nrPck = 0

    private val ackChannel = Channel<Int>(Channel.UNLIMITED)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_files_system, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //trebuie neaparat apelat o singura data - poate daca fac un init sau aici
        startAckServer()

        filePicker =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val selectedFileUri = result.data?.data
                    selectedFileUri?.let { uri ->
                        Log.i(TAG, "a deschis fisierul asta $uri")

                        //reset value
                        val textEncodeDataButtonPressed =
                            view.findViewById<TextView>(R.id.filePrintableData)
                        textEncodeDataButtonPressed.text = ""

                        //reset daca a fost ales cumva un alt fiseri intre timp
                        stringBuilder.clear()
                        nrPck = 0

                        lifecycleScope.launch {
                            deleteAllDatabaseEntries(requireContext())
                            val success = readFileContent(requireContext(), uri)
                            if (success) {
                                Log.i(TAG, "File read successfully")
                            } else {
                                Log.i(TAG, "Failed to read file")
                            }
                        }
                    }
                }
            }

        val chooseFileButton: Button = view.findViewById(R.id.chooseFile)
        chooseFileButton.setOnClickListener {
            Log.e(TAG, "apas file")
            pickFile()
            processData = true
        }

        val buttonFile: Button = view.findViewById(R.id.encodeDataFromFile)
        buttonFile.setOnClickListener {

            if (processData) {
                Log.d(TAG, "caut string $stringBuilder")
                //val textEncodeDataButtonPressed = view.findViewById<TextView>(R.id.filePrintableData)
                lifecycleScope.launch {
                    fetchLocation()
                    //delay(1000)
                    //send first pck x times - each time will update the payload with the remaining time
                    val database = PackageDataDB.getDatabase(requireContext())
                    val dao = database.dao
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
                    //dao.getPckDataBySEQnr().collect { packageDataList ->
                    val packageDataList = dao.getPckDataBySEQnr().first()
                    val iterator = packageDataList.iterator()
                    val timeSource = TimeSource.Monotonic

                    while (iterator.hasNext()) {
                        // time0 = time
                        val mark = timeSource.markNow()
                        val packageData = iterator.next()
                        val content = packageData.content
                        Log.d(TAG, "Processing content: $content")
                        localQRGenerate(packageData)
                        // delta = time - time0  = > delay(SenderReaderVars.packetDelayMs - (timeSource.markNow() - mark).inWholeMilliseconds) // - delta

                        // wait ACK pentru pachetul primit
                        val ackId = ackChannel.receive()
                        if (ackId != packageData.pckId) {
                            Log.e("SYNC", "Pachetul primit ($ackId) nu corespunde cu cel trimis (${packageData.pckId})")
                            break
                        }
                    }

                    requireView().findViewById<ImageView>(R.id.imageQR).setImageBitmap(null)
                    //textEncodeDataButtonPressed.text = stringBuilder.toString()
                    processData = false
                }

            } else {
                val textEncodeDataButtonPressed =
                    view.findViewById<TextView>(R.id.filePrintableData)
                textEncodeDataButtonPressed.text = "No file to encode"
            }
        }
    }

    private fun startAckServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val serverSocket = ServerSocket(SenderReaderVars.PORT)
                val client = serverSocket.accept()
                while (true) {
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val ack = reader.readLine()
                    Log.d("ACK_SERVER", "Primit: $ack")
                    if (ack.startsWith("ACK:")) {
                        val id = ack.removePrefix("ACK:").toIntOrNull()
                        id?.let { ackChannel.send(it) }
                        if (id == -1){
                            client.close()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ACK LA SENDER", "Eroare server", e)
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun deleteAllDatabaseEntries(context: Context) {
        val database = PackageDataDB.getDatabase(context)
        val dao = database.dao
            withContext(Dispatchers.IO) {
                dao.deleteAll()
            }
            Log.d(TAG, "All database entries deleted")
    }

    fun getLocalIpAddress(): String {
        val wm = requireContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        return String.format("%d.%d.%d.%d", ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff)
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
        localQRGenerate(PackageData(
            0,
            0,
            5 + time.toString().length
                    + 6 + nrPck.toString().length
                    + 11 + SenderReaderVars.packetDelayMs.toString().length
                    + 3 + ip.toString().length,
            stringBuilder.toString()
        ))

        Log.d("?","timpul de procesaer si cum arata payload : " + stringBuilder.toString() + " " +time + "cate pachete " + nrPck)
    }

    /* Loop to send first pck */

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun fetchLocation() {

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
            return
        }
    }

    private fun pickFile() {
        val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
        filePicker.launch(intent)
    }

    private suspend fun readFileContent(context: Context, uri: Uri?): Boolean {
        val database = PackageDataDB.getDatabase(context)

        var chunkSize =
            SenderReaderVars.payloadLength - SenderReaderVars.exceptPayload //chr per pachet(bytes?)

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
                                PackageData(
                                    i,
                                    crc,
                                    chunkString.length,
                                    chunkString
                                )
                            )
                        }

                        Log.d("important = lungime string dupa iso...", chunkString.length.toString())
                        //Log.d("important", chunkString.length.toString())
                        Log.d("gen 1/2 pck", i.toString() + " " + chunkString + " " + "caut ce adaug in db   ")
                        nrPck++;
                    }
                }
                //Log.d(TAG, "Conținutul fișierului: $stringBuilder")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Eroare la citirea fișierului", e)
                false
            }
        } else {
            return false
        }
    }

    fun localQRGenerate(packageData: PackageData): Boolean {

        val windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

            requireView().findViewById<ImageView>(R.id.imageQR).setImageBitmap(bitmap)
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
