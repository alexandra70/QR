package com.example.myqrapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.lang.Boolean.FALSE
import java.lang.Boolean.TRUE



class FilesSystem : AppCompatActivity() {


    private val stringBuilder = StringBuilder()
    private lateinit var filePicker: ActivityResultLauncher<Intent>
    private var processData : Boolean = FALSE
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_files_system)

        //initializare launcher
        filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedFileUri = result.data?.data
                selectedFileUri?.let { uri ->
                    val filePath = uri.toString()
                    Log.e(TAG, "a deschis fisierul asta" + filePath.toString())
                    readFileContent(uri)
                }
            } else {
                //sa fac ceva daca nu selecteaza nimic?
                //Toast.makeText(this, "Nu a fost selectat niciun fișier.", Toast.LENGTH_SHORT).show()
            }
        }

        val chooseFileButton: Button = findViewById(R.id.chooseFile)
        chooseFileButton.setOnClickListener {
            //startActivity(intent)
            Log.e(TAG, "apas file")
            pickFile()
            processData = TRUE //am date de procesat - daca se apasa butonu de encodare
        }

        //encode text
        val buttonFile: Button = findViewById(R.id.encodeDataFromFile)
        buttonFile.setOnClickListener {
            //startActivity(intent)
            if (processData == TRUE) {
                Log.e(TAG, "encodez")
                processDataBuffer()
                val textEncodeDataButtonPressed = findViewById<TextView>(R.id.filePrintableData)
                textEncodeDataButtonPressed.text = stringBuilder.toString()
            }
            else {
                val textEncodeDataButtonPressed = findViewById<TextView>(R.id.filePrintableData)
                textEncodeDataButtonPressed.text = "Ai apasat encodare, niciun fisier de encodat"
            }
        }

        //reset the data - trebuie sa aleg din nou un fisier daca vreau sa encodez din
        //nou ? - sau cred ca mai bine encodez tot asta la infinit pana
        //la urmatoare selectie...
        //processData = FALSE

        //open the file system
        //openFileChooser()
    }

    private fun processDataBuffer(){
        Log.e(TAG, "caut string" + stringBuilder.toString())
    }

    private fun pickFile() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        filePicker.launch(intent)
    }

    fun readFileContent(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(inputStream?.reader())
            reader.use {
                var line = it.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = it.readLine()
                }
            }
            Log.d(TAG, "Conținutul fișierului: " + stringBuilder.toString())
        } catch (e: IOException) {
            Log.e(TAG, "Eroare la citirea fișierului", e)
        }
    }

    fun openFileChooser() {
        val btnBack: Button = findViewById(R.id.chooseFile)
        btnBack.setOnClickListener {

            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)

            startActivityForResult(Intent.createChooser(intent, "Select a file"), 111)

        }
    }
}


