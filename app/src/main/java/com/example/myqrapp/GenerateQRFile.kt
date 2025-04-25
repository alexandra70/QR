package com.example.myqrapp

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient

class GenerateQRFile : AppCompatActivity() {

    lateinit var receiver: AirplaneModeChangedReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr_file)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "GENERATE QR file"

        receiver = AirplaneModeChangedReceiver()
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
