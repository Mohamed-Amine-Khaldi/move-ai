package com.example.moveai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etUsernameReg = findViewById<EditText>(R.id.etUsernameReg)
        val etPasswordReg = findViewById<EditText>(R.id.etPasswordReg)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginRedirect = findViewById<TextView>(R.id.tvLoginRedirect)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val username = etUsernameReg.text.toString().trim()
            val password = etPasswordReg.text.toString()

            if (name.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {

                // Save user information to recall during login
                val sharedPreferences = getSharedPreferences("MoveAI_Prefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putString("saved_name", name) // Added this to remember the actual name
                    .putString("saved_username", username)
                    .putString("saved_password", password)
                    .apply()

                // Save the actual name in MoveAI_Auth to be directly loaded by the Home page
                val authPrefs = getSharedPreferences("MoveAI_Auth", Context.MODE_PRIVATE)
                authPrefs.edit().putString("userName", name).apply()

                Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvLoginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}