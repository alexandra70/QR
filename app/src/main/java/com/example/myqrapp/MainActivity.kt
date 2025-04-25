package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button: Button = findViewById(R.id.button_generate_qr)

        button.setOnClickListener {
            val intent = Intent(this@MainActivity, GenerateQR::class.java)
            startActivity(intent)
        }

        val button1: Button = findViewById(R.id.button_read_qr)
        button1.setOnClickListener {
            val intent = Intent(this, ReaderQR::class.java)
            startActivity(intent)
        }

    }
}