package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.os.SystemClock
import android.os.Trace
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.TimeSource

open class ReaderQR : AppCompatActivity() {

    val timeSource = TimeSource.Monotonic /* for sleep (alternatives?)) */
    private lateinit var previewView: PreviewView
    private lateinit var resultTextView: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var cameraProvider: ProcessCameraProvider? = null

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
    //private val currentFrameId = AtomicInteger(0)

    private val prevFrameId = AtomicInteger(0)

    var timeBeforeSeq = AtomicInteger(0)
    var framesTime = AtomicInteger(0)

    var ableToProc = AtomicBoolean(false)

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

                        } else {
                            processImageProxySeq(proxy)
                            //Log.d("ReaderQR.kt","a procesat pachetul din seq " + nrPckToProcess.get())
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
                            //firstFrame.set(1)
                            val timeAux = extractTime(result)
                            timeBeforeSeq.set(timeAux)
                            nrPckToProcess.set(extractNrPck(result))
                            framesTime.set(extractFramesTime(result))

                             runOnUiThread {
                                 resultTextView.text = "res: $result"
                             }

                            Log.d("ReaderQR.kt", "[timp inceptu]= " + timeBeforeSeq.get() + " [nr pck]= " + nrPckToProcess.get() + " [trimp intre cadre]= " + framesTime.get())
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
                    //Log.d("[nu primul cadru]","ce oridine e asta  ?")
                }

                imageProxy.close()
            }
        } catch (e: Exception) {
            //imageProxy.close()
        }
    }

    /* Used for the SEQ-FRAMES */
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

            val startPhotoMedia = SystemClock.elapsedRealtime()
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val endPhotoMedia = SystemClock.elapsedRealtime()
            Log.d("Timing", "Durata Photo Media: ${endPhotoMedia - startPhotoMedia} ms")

            Trace.beginSection("BarcodeScanner processing")

            //process the image
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    Trace.endSection()

                    if (isDestroyed) return@addOnSuccessListener
                    barcodes.firstOrNull()?.rawValue?.let { result ->
                        //Log.d("[imediat dupa ce a citit un qr]=", result)
                        if ((firstFrame.get() == 1) && (nrPckToProcess.get() > 0)) {

                            //val start = System.currentTimeMillis()

                            val startDeserialize = SystemClock.elapsedRealtime()
                            val pck = PackageData.deserializePck(
                                result)
                            val endDeserialize = SystemClock.elapsedRealtime()
                            Log.d("Timing", "Durata deserializare: ${endDeserialize - startDeserialize} ms")

                            //in caul in care citesc acelasi pachet - ii dau dorop

                            if(pck.pckId == (prevFrameId.get())) {
                                Log.d("ReaderQR.kt", "Pachet ${pck.pckId} recitit - ignore")
                            }
                            else if (pck.pckId == (prevFrameId.get() + 1)) {
                                prevFrameId.set(pck.pckId)

                                Log.d("---------", "procesare pachet" + result)

                                nrPckToProcess.getAndDecrement()

                                runOnUiThread {
                                    resultTextView.text = "res: $result"
                                }

                                 lifecycleScope.launch {
                                     val startInsert = SystemClock.elapsedRealtime()
                                    databaseR.DAO_RECEIVED_PACKAGE.insertPck(
                                        ReceivedPackage(
                                            pck.pckId,
                                            pck.crc,
                                            pck.length,
                                            pck.content
                                        )
                                    )
                                     val endInsert = SystemClock.elapsedRealtime()
                                     Log.d("Timing", "trebuie sa mut pe threadul cu insert: ${endInsert - startInsert} ms")

                                 }


                                if (nrPckToProcess.get() == 0) {
                                    /* go back to parent activity */
                                    Log.d(
                                        "ReaderQR.kt",
                                        "Toate pachetele procesate - închidem camera manual"
                                    )


                                    runOnUiThread {
                                        resultTextView.text = "Succes"
                                    }

                                    lifecycleScope.launch {
                                        delay(5000)

                                        cameraProvider?.unbindAll()
                                        finish()
                                    }

                                }
                            } else {
                                //primire pachet care nu se afla in secventa
                                cameraProvider?.unbindAll()
                                barcodeScanner.close()

                                runOnUiThread {
                                    resultTextView.text = "Eroare: pachetul primit (${pck.pckId}) nu e în secvență! Te rog reia procesul de scanare ..."
                                }
                                lifecycleScope.launch {
                                    delay(5000)
                                    finish()
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
                    imageProxy.close()

                }
        } catch (e: Exception) {
            //imageProxy.close()
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

    private fun extractTime(scannedText: String): Int {

        val pck = PackageData.deserializePck(scannedText)
        val indTime = pck.content.indexOf("Time:") + 5
        val indNrPck = pck.content.indexOf("NrPck:") // len = 6
        Log.d("ReaderQR.kt"," aici un pck " + pck.pckId.toString() + " " + pck.crc + " " + pck.length + " " + pck.content)
        return pck.content.substring(indTime, indNrPck).toInt()
    }

    private fun extractNrPck(scannedText: String): Int {
        val pck = PackageData.deserializePck(scannedText)
        val indNrPck = pck.content.indexOf("NrPck:") + 6
        val indFramesTime = pck.content.indexOf("FramesTime:") // len = 6
        return pck.content.substring(indNrPck, indFramesTime).toInt()
    }

    private fun extractFramesTime(scannedText: String): Int {
        val pck = PackageData.deserializePck(scannedText)
        return pck.content.substringAfterLast("FramesTime:").toInt()
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun deleteAllDatabaseEntries() {
        withContext(Dispatchers.IO) {
            daoR.deleteAll()
        }
        Log.d(ContentValues.TAG, "All database entries deleted")
    }
}