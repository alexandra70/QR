package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.os.Trace
import android.util.Base64
import android.util.Log
import android.util.Size
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.TimeSource

open class ReaderQR : AppCompatActivity() {

    val timeSource = TimeSource.Monotonic /* for sleep (alternatives?)) */
    private lateinit var previewView: PreviewView
    private lateinit var resultTextView: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var databaseR: BytePackageDataDB
    private lateinit var daoR: BytePackageDataDao
    private lateinit var connectionSocket: Socket
    private var outPrintWriter: PrintWriter? = null
    private var readFailureCounter: Int = 0

    private val firstFrame = AtomicInteger(0)
    private val prevFrameId = AtomicInteger(0)
    private val endFrame = AtomicInteger(0)

    //todo
    private var PORT: Int = -1
    lateinit var ip: String
    lateinit var fileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_qr)

        previewView = findViewById(R.id.previewView)
        resultTextView = findViewById(R.id.resultTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        //barcodescanner to process the image
        barcodeScanner = BarcodeScanning.getClient()

        /* Init dataBase for received pck */
        databaseR = BytePackageDataDB.getDatabase(applicationContext)
        daoR = databaseR.dao

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                lifecycleScope.launch {
                    deleteAllDatabaseEntries()
                    startCamera()
                }
            } else {
                resultTextView.text = "Camera Permission is required"
            }
        }
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            lifecycleScope.launch {
                try {
                    cameraProvider = cameraProviderFuture.get()

                    bindCameraUseCases() //Suspension functions can be called only within coroutine body

                } catch (exc: Exception) {
                    Log.e("Camera", "INIT CAMERA ERROR - 4?", exc)
                    resultTextView.text = "Error: camera not working"
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        try {
            provider.unbindAll()

            val screenSize = Size(1280, 720)
            //if this size for the screen is not available then use no other resolution strategy => FALLBACK_RULE_NONE
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        screenSize,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()

            //to allow live camera feedback to the screen
            //attach the preview of the camera to the UI preview = > automatic detection of qr code
            val preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setResolutionSelector(resolutionSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { proxy ->

                        if ((firstFrame.get() == 0)) {
                            processImageProxy(proxy)

                        } else {
                            processImageProxySeq(proxy)
                        }
                    }
                }

            //select default camera - > back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            //link image to the preview to the image-analyzer(process frames to analyze simultaneously)
            provider.bindToLifecycle(this,
                cameraSelector, preview, imageAnalyzer)

        } catch (exc: Exception) {
            Log.e("Camera", " error ", exc)
        }
    }

    /* Used for the FIRST FRAME */
    /* it is called for EACH frame scanned by the camera */
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            //image is converted into an input image
            //rotationDegrees used for accurate scan - image is rotated correctly
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            //process the image
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (isDestroyed) return@addOnSuccessListener
                    barcodes.firstOrNull()?.rawValue?.let { result ->

                        if (firstFrame.compareAndSet(0,1)) {
                            /* Store the time for the first frame */
                            /* Scanning process should start after timeBeforeSeq + latestAnalyzedTimestamp is >= than
                                * the time stamp of current analyzed photo */
                            Log.d("FIRST", "here")

                            if (checkFirstPck(result)) {

                                fileName = extractFileName(result)
                                ip = extractIp(result)
                                PORT = extractPORT(result)

                                runOnUiThread {
                                    resultTextView.text = "res: $result"
                                }

                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        Log.d("Before CONNECTED", "inaininte")
                                        connectionSocket = Socket(ip, PORT)
                                        Log.d("CONNECTED", "S-a conectat")
                                        sendAckToSender(connectionSocket, 0, 0)

                                        outPrintWriter = PrintWriter(connectionSocket.getOutputStream(), true)

                                    } catch (e: Exception) {
                                        Log.e("RETEA", "Eroare la conectare socket", e)
                                    }
                                }
                            } else {
                                //nu am inca primul pachet corect
                                firstFrame.getAndSet(0)
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (!isDestroyed) {
                        //imageProxy.close()
                        resultTextView.text = "Wait to scan a qr code"
                    }
                }
            .addOnCompleteListener {
            //clean up memory and allow nxt frame to pe processed
            //after the task was completed the addOnCompleteListener is called
                if(firstFrame.get() == 0) {
                    //imageProxy.close()
                }

                imageProxy.close()
            }
        } catch (e: Exception) {
            //imageProxy.close()
        }
    }

    private suspend fun writeToFile() {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        //val file = File(downloadsDir, "compus")
        val file = File(downloadsDir, fileName)

        val outputStream = withContext(Dispatchers.IO) {
            FileOutputStream(file)
        }

        val pck = daoR.getPckDataBySEQnr().first()

        val iterator = pck.iterator()
        while (iterator.hasNext()) {
            val packageData = iterator.next()
            //val content = packageData.content
            //val bytes = packageData.content.toByteArray(Charsets.ISO_8859_1)

            withContext(Dispatchers.IO) {
                outputStream.write(packageData.byteArray)
            }

        }
        withContext(Dispatchers.IO) {
            outputStream.close()
        }
        Log.d("RECONSTRUCT", "Fișier salvat la: ${file.absolutePath}")
    }

    private fun checkFirstPck(result: String): Boolean{
        if (!result.contains("ID:")) {
            Log.d("Deserializer", "Eroare: Lipseste campul ID")
            return false
        }

        if (!result.contains("CRC:")) {
            Log.d("Deserializer", "Eroare: Lipseste campul CRC")
            return false
        }

        if (!result.contains("Length:")) {
            Log.d("Deserializer", "Eroare: Lipseste campul Length")
            return false
        }

        if (!result.contains("Payload:")) {
            Log.d("Deserializer", "Eroare: Lipseste campul Payload")
            return false
        }

        return true
    }

    /* Used for the SEQ-FRAMES */
    /* it is called for EACH frame scanned by the camera */
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxySeq(imageProxy: ImageProxy) {

        try {
            val mediaImage = imageProxy.image ?: run {
                readFailureCounter += 1
                if (readFailureCounter >= SenderReaderVars.FAIL_COUNT_TRIGGER){
                    sendAckToSender(connectionSocket, -2, 0)
                    readFailureCounter = 0
                }
                imageProxy.close()
                return
            }

            //image is converted into an input image
            //rotationDegrees used for accurate scan - image is rotated correctly
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            //process the image
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Trace.endSection()

                    if (barcodes.isEmpty()) {
                        Log.d(
                            "ReaderQR.kt",
                            "Poza neclara"
                        )
                        readFailureCounter += 1
                        if (readFailureCounter >= SenderReaderVars.FAIL_COUNT_TRIGGER) {
                            sendAckToSender(connectionSocket, -2, 0)
                            readFailureCounter = 0
                        }
                    }

                    if (isDestroyed) return@addOnSuccessListener
                    barcodes.firstOrNull()?.rawValue?.let { result ->
                        //Log.d("[imediat dupa ce a citit un qr]=", result)
                        if (firstFrame.get() == 1) {
                            // if strcmp (PackadeData, "END") != 0
                            readFailureCounter = 0
                            if (result == "END") {
                                if(endFrame.compareAndSet(0,1)) {
                                    sendAckToSender(connectionSocket, -1, 0)

                                    /* go back to parent activity */
                                    runOnUiThread {
                                        resultTextView.text = "Success"
                                    }

                                    connectionSocket.close()

                                    //scriu fisier
                                    lifecycleScope.launch {
                                        writeToFile()
                                        delay(5000)
                                        cameraProvider?.unbindAll()
                                        finish()
                                    }
                                }
                            } else {

                                val pck = PackageData.deserializePck(
                                    result
                                )
                                if (pck.pckId == (prevFrameId.get())) {
                                    Log.d("ReaderQR.kt", "Pachet ${pck.pckId} recitit - ignore")
                                } else if (pck.pckId == (prevFrameId.get() + 1)) {
                                    prevFrameId.set(pck.pckId)

                                    runOnUiThread {
                                        resultTextView.text = "res: $result"
                                    }

                                    // TODO check CRC

                                    lifecycleScope.launch {
                                        var pckBytesArray = Base64.decode(pck.content, Base64.NO_WRAP)
                                        databaseR.dao.insertPck(
                                            BytePackageData(
                                                pck.pckId,
                                                pckBytesArray.size,
                                                pckBytesArray
                                            )
                                        )
                                        //trimit ack si dupa verific cat timp a trecut
                                        sendAckToSender(connectionSocket, pck.pckId, pckBytesArray.size)
                                    }

                                } else {
                                    //primire pachet care nu se afla in secventa

                                    runOnUiThread {
                                        resultTextView.text =
                                            "Eroare: pachetul primit (${pck.pckId}) nu e in secvența! Te rog reia procesul de scanare ..."
                                    }

                                    cameraProvider?.unbindAll()
                                    barcodeScanner.close()

                                    lifecycleScope.launch {
                                        delay(5000)
                                        finish()
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (!isDestroyed) {
                        //imageProxy.close()
                        readFailureCounter += 1
                        if (readFailureCounter >= SenderReaderVars.FAIL_COUNT_TRIGGER){
                            sendAckToSender(connectionSocket, -2, 0)
                            readFailureCounter = 0
                        }
                        resultTextView.text = "Wait to scan a qr code"
                    }
                }
                .addOnCompleteListener {
                    //clean up memory and allow nxt frame to pe processed
                    //after the task was completed the addOnCompleteListener is called
                    imageProxy.close()

                }
        } catch (e: Exception) {
            //imageProxy.close()
        }
    }

    private fun sendAckToSender(socket: Socket, pckId: Int, pckBytesArrayLength : Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("sendAckToSender", "inainte sa trimit ack reader")
                outPrintWriter?.println("ACK:$pckId:$pckBytesArrayLength")
                Log.d("sendAckToSender", "dupa ce am trimis ack reader")
            } catch (e: Exception) {
                Log.e("ACK READER QR", "nu merge ack in reader....", e)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("ReaderQR", "onStop called")

        //lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("sendAckToSender", "premature destroy")
                outPrintWriter?.println("ACK:-1:0")
                connectionSocket.close()
            } catch (e: Exception) {
                Log.e("ACK READER QR", "...", e)
            }
        //}
        try {
            Log.d("ReaderQR", "onStop - shutting down camera and executor")
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            barcodeScanner.close()
        } catch (e: Exception) {
            Log.e("ReaderQR", "Error during shutdown in onStop()", e)
        }
    }

    override fun onDestroy() {

        super.onDestroy()
        Log.d("ReaderQR", "onDestroy called")
        /*cameraExecutor.shutdown()
        cameraExecutor.awaitTermination(1, TimeUnit.SECONDS)
        cameraProvider?.unbindAll()
        barcodeScanner.close()*/

        /*
        try {
            connectionSocket.close()
        } catch (e: Exception) {
            Log.e("ReaderQR", "socket_stop", e)
        }*/
    }

    private fun extractFileName(scannedText: String): String {
        val pck = PackageData.deserializePck(scannedText)
        val indFileName = pck.content.indexOf("FileName:") + 9
        val indIP = pck.content.indexOf("IP:")
        return pck.content.substring(indFileName, indIP)
    }
    private fun extractIp(scannedText: String): String {

        val pck = PackageData.deserializePck(scannedText)
        val indIP = pck.content.indexOf("IP:") + 3
        val indPORT = pck.content.indexOf("PORT:")
        return pck.content.substring(indIP, indPORT)
        return pck.content.substringAfterLast("IP:")
    }
    private fun extractPORT(scannedText: String): Int {
        val pck = PackageData.deserializePck(scannedText)
        return pck.content.substringAfterLast("PORT:").toInt()
    }
    @SuppressLint("SuspiciousIndentation")
    private suspend fun deleteAllDatabaseEntries() {
        withContext(Dispatchers.IO) {
            daoR.deleteAll()
        }
        Log.d(ContentValues.TAG, "All database entries deleted")
    }
}