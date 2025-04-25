package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    //?
    //sterge linie
    private lateinit var databaseR: PackageReceivedDB
    private lateinit var daoR: DaoReceivedPackage

    var latestAnalyzedTimestamp = 0L;
    /*
    A not yet analyzed first frame
    After the first frame is read and the time is extracted this should
    have the value eq to one - that means the next first-frame-seq will be dropped
    */
    //var analyzedFirstImage = 0;

    private val firstFrame = AtomicInteger(0)
    private val nrPckToProcess = AtomicInteger(0)
    var timeBeforeSeq = AtomicInteger(0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_qr)

        previewView = findViewById(R.id.previewView)
        resultTextView = findViewById(R.id.resultTextView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        //barcodescanner to process the image
        barcodeScanner = BarcodeScanning.getClient()

        /* Init dataBase for received pck */
        //sterge linie
        databaseR = PackageReceivedDB.getDatabase(applicationContext)
        daoR = databaseR.DAO_RECEIVED_PACKAGE

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                lifecycleScope.launch {
                    //sterg baza de date inainte sa o folosesc
                    //sterge linie
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
                //.setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                //.setImageQueueDepth(1) //new
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { proxy ->

                        if ((firstFrame.get() == 0)) {
                            processImageProxy(proxy)

                            Log.d("ReaderQR.kt","chestia asta se opresete cand frist e 1")

                        } else {
                            processImageProxySeq(proxy)
                            Log.d("ReaderQR.kt","a procesat pachetul din seq " + nrPckToProcess.get())
                        }

                    }
                }

            //select default camera - > back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            //link image to the preview to the image-analyzer(process frames to analyze simultaneously)
            provider.bindToLifecycle(this,
                cameraSelector, preview, imageAnalyzer)

        } catch (exc: Exception) {
            Log.e("Camera", "err?? ", exc)
        }
    }

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
                        runOnUiThread {
                            resultTextView.text = "res: $result"

                            if (firstFrame.get() == 0) {
                                /* Store the time for the first frame */
                                /* Scanning process should start after timeBeforeSeq + latestAnalyzedTimestamp is >= than
                                * the time stamp of current analyzed photo */

                                val timeAux = extractTime(result)
                                timeBeforeSeq.set(timeAux)
                                Log.d("ReaderQR.kt","am setat yimpul la??" + timeBeforeSeq.get() + "nr pck = " + nrPckToProcess.get())
                                firstFrame.set(1)
                                nrPckToProcess.set(extractNrPck(result))

                                lifecycleScope.launch {
                                    delay(timeBeforeSeq.get().toLong())
                                    Log.d("ReaderQR.kt","aici trebuie inchisa poza altfel nu o sa mearga sincronizarea ?")

                                    //daca inchid poza aici dupa delay
                                    //trece sa faca citire pt firs == read and nrpck>0
                                    imageProxy.close()
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    if (!isDestroyed) {
                        resultTextView.text = "Wait to scan a qr code"
                    }
                }
            .addOnCompleteListener {
            //clean up memory and allow nxt frame to pe processed
            //after the task was completed the addOnCompleteListener is called
                if(firstFrame.get() == 0) {
                    imageProxy.close()
                    Log.d("ReaderQR.kt","ce oridine e asta  ?")
                }
            }
        } catch (e: Exception) {
            imageProxy.close()
        }
    }


    /* it is called for EACH frame scanned by the camera */
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxySeq(imageProxy: ImageProxy) {
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
                        runOnUiThread {
                            resultTextView.text = "res: $result"

                            if ((firstFrame.get() == 1) && (nrPckToProcess.get() > 0)) {

                                lifecycleScope.launch {
                                    val pck = PackageData.deserializePck(result)


                                    //if pck.id != anterior + 1
                                    //   { //to do - opresc operatia NU MAI SCANEZ SI AFESEZ PE
                                    //ECRAN  afiseaza => MESAJ ORIDINEA GRESITA


                                    //if pck.crc != crc(pck.payload)
                                    //ECRAN A APRAUT afiseaza
                                    // => MESAJ PACHET CORUPT - CORRUPT DATA



                                    Log.d("ReaderQR.kt","procesare pachet" + result)
                                    val mark = timeSource.markNow()
                                    var nrPckToProcessAux = nrPckToProcess.get()
                                    nrPckToProcessAux = nrPckToProcessAux - 1
                                    nrPckToProcess.set(nrPckToProcessAux);

                                    Log.d("ReaderQR.kt", "mai am vreun pachet de procesat?? " + nrPckToProcess)

                                    //sterge linie
                                    databaseR.DAO_RECEIVED_PACKAGE.insertPck(
                                        ReceivedPackage(
                                            pck.pckId,
                                            pck.crc,
                                            pck.length,
                                            pck.content
                                        )
                                    )

                                    delay(SenderReaderVars.packetDelayMs - (timeSource.markNow() - mark).inWholeMilliseconds)


                                    //mai optim sa o fac inainte??
                                    if (nrPckToProcess.get() == 0) {
                                        /* go back to parent activity */

                                        Log.d("ReaderQR.kt", "Toate pachetele procesate - închidem camera manual")

                                        cameraProvider?.unbindAll()  // Oprim camera înainte de a distruge UI-ul
                                        delay(100)                   // Dăm timp camerei să se elibereze

                                        finish()
                                    } else {
                                        imageProxy.close()
                                    }
                                }
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
                        imageProxy.close()
                        Log.d("ReaderQR.kt","ce oridine e asta  ?")
                    }
                }
        } catch (e: Exception) {
            imageProxy.close()
        }
    }


    override fun onStop() {
        super.onStop()
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
    }

    private fun processFirstFrame(scannedText: String) {

        val timeAux = extractTime(scannedText)
        timeBeforeSeq.set(timeAux)
        Log.d("ReaderQR.kt","am setat yimpul la??" + timeBeforeSeq.get())
        firstFrame.set(1)
        nrPckToProcess.set(extractNrPck(scannedText))
        //firstFrame = 1//first pck received
    }

    private fun extractTime(scannedText: String): Int {

        val pck = PackageData.deserializePck(scannedText)
        val indTime = pck.content.indexOf("Time:") + 5
        val indNrPck = pck.content.indexOf("NrPck:") // len = 6
        Log.d("ReaderQR.kt"," aici un pck " + pck.pckId.toString() + " " + pck.crc + " " + pck.length + " " + pck.content)

        return pck.content.substring(indTime, indNrPck).toInt()
    }

    private fun extractNrPck(scannedText: String): Int {

        val pck = PackageData.deserializePck(scannedText)
        return pck.content.substringAfterLast("NrPck:").toInt()
    }


    @SuppressLint("SuspiciousIndentation")
    private suspend fun deleteAllDatabaseEntries() {

        withContext(Dispatchers.IO) {
            daoR.deleteAll()
        }
        Log.d(ContentValues.TAG, "All database entries deleted")
    }


}