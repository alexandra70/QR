package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

open class GenerateQR : AppCompatActivity() {

    lateinit var receiver: AirplaneModeChangedReceiver

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "GENERATE QR MENU"

        val buttonText: Button = findViewById(R.id.button_generate_qr_text)
        buttonText.setOnClickListener {
            val intent = Intent(this@GenerateQR, GenerateQRText::class.java)
            startActivity(intent)
        }

        val buttonFile: Button = findViewById(R.id.file_system)
        buttonFile.setOnClickListener {
            replaceFragment(FilesSystem())
        }

        receiver = AirplaneModeChangedReceiver()
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
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
