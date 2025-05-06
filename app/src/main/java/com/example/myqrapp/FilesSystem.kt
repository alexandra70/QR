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
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.zip.CRC32
import kotlin.time.TimeSource

class FilesSystem : Fragment() {

    private val stringBuilder = StringBuilder()
    private lateinit var filePicker: ActivityResultLauncher<Intent>
    private var processData: Boolean = false
    private var nrPck = 0

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
                    //while (timeStart != 0) {
                    while (timeStart > 0) {
                        val startTime = SystemClock.elapsedRealtime()
                        processFirstPck(timeStart)
                        timeStart = timeStart - SenderReaderVars.firstPacketRepeatInterval

                        val endTime = SystemClock.elapsedRealtime()
                        val processTime = endTime - startTime

                        //delay(SenderReaderVars.firstPacketRepeatInterval)
                        Log.d(
                            "uite",
                            (SenderReaderVars.firstPacketRepeatInterval - processTime).toString()
                        )
                        val remainingDelay =
                            SenderReaderVars.firstPacketRepeatInterval - processTime
                        if (remainingDelay > 0) {
                            delay(remainingDelay)
                        }
                    }

                    val endTimeTest = SystemClock.elapsedRealtime()

                    Log.d(
                        "cand se terimna, dar diferenta",
                        (endTimeTest - startTimeTest).toString()
                    )

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
                        // delta = time - time0
                        delay(SenderReaderVars.packetDelayMs - (timeSource.markNow() - mark).inWholeMilliseconds) // - delta
                        //}

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

    @SuppressLint("SuspiciousIndentation")
    private suspend fun deleteAllDatabaseEntries(context: Context) {
        val database = PackageDataDB.getDatabase(context)
        val dao = database.dao
            withContext(Dispatchers.IO) {
                dao.deleteAll()
            }
            Log.d(TAG, "All database entries deleted")
    }

    private fun processFirstPck(time: Long) {

        val stringBuilder = StringBuilder()
        stringBuilder.append("Time:")
        stringBuilder.append(time.toString())

        stringBuilder.append("NrPck:")
        stringBuilder.append(nrPck.toString())

        stringBuilder.append("FramesTime:")
        stringBuilder.append(SenderReaderVars.packetDelayMs)

        //send data
        localQRGenerate(PackageData(
            0,
            0,
            5 + time.toString().length
                    + 6 + nrPck.toString().length
                    + 11 + SenderReaderVars.packetDelayMs.toString().length,
            stringBuilder.toString()
        ))

        println("timpul de procesaer si cum arata payload : " + stringBuilder.toString() + " " +time + "cate pachete " + nrPck)
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

        var chunkSize = SenderReaderVars.payloadLength //chr per pachet(bytes?)

        println("aici fisier" + uri.toString())

        if (uri != null) {
            return try {
                val inputStream =
                    withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri) }
                var bytesRead: Int
                val buffer = ByteArray(chunkSize)
                var i = 0

                println("deci nu e null - ar trebui sa intre sa proceseze content din file")

                    inputStream?.use { stream ->
                        while (withContext(Dispatchers.IO) {
                                stream.read(buffer).also { bytesRead = it }
                            } != -1) {
                            val chunk = buffer.copyOfRange(0, bytesRead)
                            val chunkString = String(chunk)
                            stringBuilder.append(chunkString)
                            val crc = calculateCRC(chunk)
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
                            println(i.toString() + " " + chunkString + " " + "caut ce adaug in db   ")
                            nrPck++;
                        }
                    }
                Log.d(TAG, "Conținutul fișierului: $stringBuilder")
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

    private fun calculateCRC(data: ByteArray): Long {
        val crc = CRC32()
        crc.update(data)
        return crc.value
    }

    private fun generateQRCode(
        data: String,
        size: Int,
        errorCorrectionLevel: ErrorCorrectionLevel
    ): Bitmap? {
        return try {
            val hints = mapOf(
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
