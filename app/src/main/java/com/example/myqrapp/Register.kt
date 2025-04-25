package com.example.myqrapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Register : Fragment() {

    private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Toast.makeText(requireContext(), "Please Enter Email and Password to register", Toast.LENGTH_SHORT).show()

        auth = Firebase.auth

        val login_activity = activity as Login
        val email_login_activity = login_activity.getActivityEditEmail()

        val password_login_activity = login_activity.getActivityEditPassword()

        val register_button_lodin_activity = login_activity.getActivityEditRegisterButton()


        register_button_lodin_activity.setOnClickListener {
            val email = email_login_activity.text.toString().trim()
            val password = password_login_activity.text.toString().trim()

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Please Enter Email and Password", Toast.LENGTH_SHORT).show()
            }
            else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "createUserWithEmail:success")
                            val intent = Intent(requireContext(), Login::class.java)
                            startActivity(intent)
                            Toast.makeText(requireContext(), "Registration Successful: $email", Toast.LENGTH_SHORT).show()
                            requireActivity().finish()
                        }
                        else {
                            Log.w(TAG, "Unable to create a new user", task.exception)
                            Toast.makeText(requireContext(), "Unable to create a new user", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

}
