package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity

open class GenerateQR : AppCompatActivity() {

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
            val intent = Intent(this@GenerateQR, FilesSystem::class.java)
            startActivity(intent)
        }

    }
}
