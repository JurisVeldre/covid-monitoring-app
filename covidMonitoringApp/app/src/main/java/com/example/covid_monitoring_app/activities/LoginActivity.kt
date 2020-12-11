package com.example.covid_monitoring_app.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.covid_monitoring_app.R
import kotlinx.android.synthetic.main.activity_login.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = Firebase.auth
        if (auth.currentUser != null) navigateToMain()
        loginButton.setOnClickListener {
            onLogin()
        }
        findViewById<TextView>(R.id.signupButton)
            .setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, RangingActivity::class.java))
    }

    private fun onLogin() {
        val email = emailField.text.toString()
        val pass = passwordField.text.toString()
        if (email.isNotEmpty() && pass.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        navigateToMain()
                    } else {
                        val errorText =
                            if (task.exception != null) task.exception?.message.toString() else "Authentication failed"
                        Toast.makeText(
                            baseContext, errorText,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}
