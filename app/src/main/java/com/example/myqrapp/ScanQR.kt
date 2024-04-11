package com.example.myqrapp


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myqrapp.databinding.ActivityMainBinding
import com.google.zxing.Result
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import me.dm7.barcodescanner.zxing.ZXingScannerView
import kotlin.properties.Delegates


class ScanQR : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private lateinit var binding: ActivityMainBinding
    private var qrR by Delegates.notNull<Int>()

    private val RequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                isGranted: Boolean ->
            if(isGranted) {
                showCamera();
            } else {
                println("need permission")
            }
        }

    private val ScanLauncher = registerForActivityResult(ScanContract()) {
            result: ScanIntentResult ->
        run {
            if(result.contents == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            } else {

                val textViewDecoded = findViewById<TextView>(R.id.textScanned)
                println(result.contents)
                textViewDecoded.text = result.contents
            }
        }

    }

    /* private fun setResult(contents: String) {
        textResult = contents
    } */

    private fun showCamera() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan QR code")
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(false)

        ScanLauncher.launch(options)
    }

    private fun initViews() {
        checkPermissionCamera(this)
        qrR = 1
    }

    private fun checkPermissionCamera(context: Context) {
        if(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showCamera()
        } else if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            Toast.makeText(context, "camera permission required", Toast.LENGTH_SHORT).show()
        } else {
            RequestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun initBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun handleResult(p0: Result?) {
        TODO("Not yet implemented")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        qrR = 0;
        initBinding()
        initViews()

        if(qrR == 1) {
            //print text on screen
            setContentView(R.layout.activity_scan_qr)
            val textScannedTextView = findViewById<TextView>(R.id.textScanned)

            textScannedTextView.movementMethod = ScrollingMovementMethod()

            qrR = 0;
        }
        setContentView(R.layout.activity_scan_qr)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "SCAN QR"

        val button: Button = findViewById(R.id.restart_scanning)
        button.setOnClickListener {
            val i = Intent(this@ScanQR, ScanQR::class.java)
            startActivity(i)
        }

    }
}