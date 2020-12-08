package com.example.covid_monitoring_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.covid_monitoring_app.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        auth = Firebase.auth
        signupButton.setOnClickListener {
            onRegister()
        }
    }

    private fun onRegister() {
        auth.createUserWithEmailAndPassword(emailField.text.toString(), passwordField.text.toString())
            .addOnCompleteListener(this) {task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, RangingActivity::class.java))
                } else if (task.isCanceled) {
                    val errorText = if (!task.isSuccessful) task.exception?.message.toString() else "Registration failed"
                    Toast.makeText(baseContext, errorText,
                        Toast.LENGTH_SHORT).show()
                }
            }
    }
}
