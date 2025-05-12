package com.example.myqrapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ScanQR : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private lateinit var mScannerView: ZXingScannerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)
        mScannerView.setResultHandler(this)
    }
    override fun onResume() {
        super.onResume()
        mScannerView.setResultHandler(this)
        mScannerView.startCamera()
    }
    override fun onPause() {
        super.onPause()
        mScannerView.stopCamera() // Stop camera on pause
    }
    override fun handleResult(rawResult: Result) {
        /* Trimite rezultatul înapoi la activitatea princip Am adăugat un Intent pentru a trimite textul scanat înapoi la activitatea principală:  setează rezultatul activității și îl trimite înapoi. */
        val resultIntent = Intent().apply {
            putExtra("scannedBytes", rawResult.text)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
