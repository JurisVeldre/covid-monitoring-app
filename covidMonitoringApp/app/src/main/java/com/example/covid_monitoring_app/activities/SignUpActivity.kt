package com.example.covid_monitoring_app.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
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
        setContentView(R.layout.activity_register)
        auth = Firebase.auth
        signupButton.setOnClickListener {
            onRegister()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Register"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }

    private fun onRegister() {
        val email = emailField.text.toString()
        val pass = passwordField.text.toString()
        if (email.isNotEmpty() && pass.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) {task ->
                    if (task.isSuccessful) {
                        startActivity(Intent(this, RangingActivity::class.java))
                    } else if (task.exception != null) {
                        val errorText = if (!task.isSuccessful) task.exception?.message.toString() else "Registration failed"
                        Toast.makeText(baseContext, errorText,
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
