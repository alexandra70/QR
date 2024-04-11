package com.example.myqrapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailEdt = findViewById<EditText>(R.id.email)
        val passwordEdt = findViewById<EditText>(R.id.password)
        val registerBtn = findViewById<Button>(R.id.btnRegister)

        registerBtn.setOnClickListener {
            if (TextUtils.isEmpty(emailEdt.text.toString()) && TextUtils.isEmpty(passwordEdt.text.toString())) {
                Toast.makeText(this@Register, "Please Enter Email and Password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener;
            } else {
                auth.createUserWithEmailAndPassword(emailEdt.toString(), passwordEdt.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success")
                            val user = auth.currentUser
                            val i = Intent(this@Register, Login::class.java)
                            startActivity(i)
                            finish() //nu revin?
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.exception)
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }
        }

       /* back.setOnClickListener {
            // Navighează către activitatea de autentificare (LoginActivity)
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
        }*/
    }
}
