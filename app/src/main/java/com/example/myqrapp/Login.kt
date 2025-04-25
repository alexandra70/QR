package com.example.myqrapp

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class Login : AppCompatActivity() {

    lateinit var receiver: AirplaneModeChangedReceiver

    var auth = Firebase.auth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Toast.makeText(this@Login, "Please Enter Email and Password to login", Toast.LENGTH_SHORT).show()

        val emailEdt = findViewById<EditText>(R.id.email)
        val passwordEdt = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.btnLogin)
        val registerBtn = findViewById<Button>(R.id.btnRegister)

        loginBtn.setOnClickListener {
            var email = emailEdt.text
            var password = passwordEdt.text

            if (TextUtils.isEmpty(emailEdt.text.toString()) && TextUtils.isEmpty(passwordEdt.text.toString())) {
                Toast.makeText(this@Login, "Please Enter Email and Password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener;
            } else {
                auth.signInWithEmailAndPassword(email.toString().trim(), password.toString().trim())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "createUserWithEmail:success" + email.toString().trim()
                                            + password.toString().trim())
                            val i = Intent(this@Login, MainActivity::class.java)
                            startActivity(i)
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure" + email.toString().trim()
                                    + password.toString().trim(), task.exception)
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }
        }

        registerBtn.setOnClickListener {
            replaceFragment(Register())
        }

        receiver = AirplaneModeChangedReceiver()
    }

    fun getActivityEditEmail(): EditText {
        return findViewById<EditText>(R.id.email)
    }

    fun getActivityEditPassword(): EditText {
        return findViewById<EditText>(R.id.password)
    }

    fun getActivityEditRegisterButton(): Button {
        return findViewById<Button>(R.id.btnRegister)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_register, fragment)
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