package com.example.moveai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("MoveAI_Prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        // Auto-Login logic
        if (isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegisterRedirect = findViewById<TextView>(R.id.tvRegisterRedirect)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            val savedUser = sharedPreferences.getString("saved_username", "")
            val savedPass = sharedPreferences.getString("saved_password", "")
            val savedName = sharedPreferences.getString("saved_name", username) // Retrieve the actual name

            if (username.isNotEmpty() && password.isNotEmpty()) {

                if (username == savedUser && password == savedPass) {

                    sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

                    // Save the actual name to MoveAI_Auth to be displayed in Home and Trainer
                    val authPrefs = getSharedPreferences("MoveAI_Auth", Context.MODE_PRIVATE)
                    authPrefs.edit().putString("userName", savedName).apply()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid Username or Password!", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegisterRedirect.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}