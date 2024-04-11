package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.net.http.HttpException
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.io.IOException

data class QRCodeRequest(
    val data: String,
    val body: String
)

interface QRCodeApiService {
    @FormUrlEncoded
    @POST("/v1/create-qr-code/")
    suspend fun createUrlPost(
        //@Field("userId") userId : Int,
        @Field("data") title: String,
        @Field("body") body: String?,
    ):Response<QRCodeRequest>

}

open class GenerateQRText : AppCompatActivity() {

    lateinit var qrIV: ImageView
    lateinit var msgEdt: EditText
    lateinit var generateQRBtn: Button
    lateinit var bitmap: Bitmap
    lateinit var qrEncoder: QRGEncoder

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr_text)

        /*val button: Button = findViewById(R.id.btnOpenMain)
        button.setOnClickListener {
            val i = Intent(this@GenerateQRText, MainActivity::class.java)
            startActivity(i)
        }
        */

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "GENERATE QR TEXT"

        qrIV = findViewById(R.id.idIVQrcode)
        msgEdt = findViewById(R.id.idEdt)
        generateQRBtn = findViewById(R.id.ButonGenerareQREfectiva)

        localQRGenerate()
        //onlineQRGenerate()

    }


    @OptIn(DelicateCoroutinesApi::class)
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    private fun onlineQRGenerate() {
        val baseUrl = "https://api.qrserver.com/v1/create-qr-code/?data=Example"

        GlobalScope.launch(Dispatchers.IO) {
            var response : Retrofit? = null
            response = try {
                //val user = User("new body",null,"new title",23)
                Retrofit.Builder().baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            } catch (e: HttpException) {
                Toast.makeText(applicationContext, "http error ${e.message}", Toast.LENGTH_LONG)
                    .show()
                return@launch
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "app error ${e.message}", Toast.LENGTH_LONG)
                    .show()
                return@launch
            }
            if (response == null) {
                Log.d("POST QR: ", "primeste raspuns dar nu are body??")
            } else {
                Log.d("CEVA: ", response.toString() + "ce  mai")
            }

        }


        Log.d(TAG, "a trecut so nu a facut minic")

       //val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=$msgEdt"))
        //startActivity(i)

       /* val url = "https://publicobject.com/helloworld.txt"

        val quotesApi = RetrofitHelper.getInstance().create(QuotesApi::class.java)
        // launching a new coroutine
        GlobalScope.launch {
            val result = quotesApi.getQuotes()
            if (result != null)
            // Checking the results
                Log.d("ayush: ", result.body().toString())
        }*/
    }

    private fun localQRGenerate() {
        generateQRBtn.setOnClickListener {
            if (TextUtils.isEmpty(msgEdt.text.toString())) {

                Toast.makeText(applicationContext, "Enter your message", Toast.LENGTH_SHORT).show()
            } else {
                val windowManager: WindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                val display: Display = windowManager.defaultDisplay
                val point = Point()

                display.getSize(point)

                val width = point.x
                val height = point.y
                var dimen = if (width < height) width else height

                dimen = dimen * 3 / 4

                qrEncoder = QRGEncoder(msgEdt.text.toString(), null, QRGContents.Type.TEXT, dimen)

                //qrEncoder.setColorBlack(Color.White);
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



}
