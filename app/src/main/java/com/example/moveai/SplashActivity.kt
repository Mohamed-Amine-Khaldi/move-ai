package com.example.moveai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Linking UI elements
        val centerContent = findViewById<LinearLayout>(R.id.centerContent)
        val bottomContent = findViewById<LinearLayout>(R.id.bottomContent)

        // Pop-up animation for the Logo and App Name
        centerContent.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(800)
            .setInterpolator(OvershootInterpolator()) // Creates the bouncy effect
            .start()

        // Fade-in animation for "from Mohamed Amine Khaldi"
        bottomContent.animate()
            .alpha(1f)
            .setDuration(1000)
            .setStartDelay(600) // Starts slightly after the logo animation
            .start()

        // Delay to wait for animations to finish, then navigate to the next screen
        Handler(Looper.getMainLooper()).postDelayed({

            // Check if the user is already logged in
            val prefs = getSharedPreferences("MoveAI_Auth", Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("isLoggedIn", false)

            if (isLoggedIn) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, 2800)
    }
}