package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Display
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity

open class GenerateQRText : AppCompatActivity() {

    lateinit var qrIV: ImageView
    lateinit var msgEdt: EditText
    lateinit var generateQRBtn: Button
    lateinit var bitmap: Bitmap
    lateinit var qrEncoder: QRGEncoder
    lateinit var receiver: AirplaneModeChangedReceiver

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr_text)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "GENERATE QR TEXT"

        qrIV = findViewById(R.id.idIVQrcode)
        msgEdt = findViewById(R.id.idEdt)
        generateQRBtn = findViewById(R.id.ButonGenerareQREfectiva)

        localQRGenerate()

        receiver = AirplaneModeChangedReceiver()
    }

    fun callEncoderForWindow(message:String, windowService:String): Boolean {

        if(message.isNullOrEmpty()) return false

        if(windowService.isNullOrEmpty()) return false

        if(windowService.compareTo("window") != 0) return false

        val windowManager: WindowManager = getSystemService(windowService) as WindowManager
        val display: Display = windowManager.defaultDisplay

        val point = Point()

        display.getSize(point)

        val width = point.x
        val height = point.y
        var dimen = if (width < height) width else height

        dimen = dimen * 3 / 4

        qrEncoder = QRGEncoder(message, null, QRGContents.Type.TEXT, dimen)
        return true
    }

    private fun localQRGenerate() {
        generateQRBtn.setOnClickListener {
            if (TextUtils.isEmpty(msgEdt.text.toString())) {
                Toast.makeText(applicationContext, "Enter your message", Toast.LENGTH_SHORT).show()
            } else {

                callEncoderForWindow(msgEdt.text.toString(), WINDOW_SERVICE)

                try {
                    bitmap = qrEncoder.getBitmap(0);
                    (findViewById<ImageView>(R.id.idIVQrcode)).setImageBitmap(bitmap);
                    qrIV.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(receiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
    }

}
