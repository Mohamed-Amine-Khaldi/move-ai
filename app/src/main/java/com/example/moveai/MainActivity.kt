package com.example.moveai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    private val trainerFragment = TrainerFragment()
    private val mapFragment = MapFragment()
    private val foodFragment = FoodFragment()

    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        try {
            com.amap.api.maps.MapsInitializer.updatePrivacyShow(this, true, true)
            com.amap.api.maps.MapsInitializer.updatePrivacyAgree(this, true)
            com.amap.api.location.AMapLocationClient.updatePrivacyShow(this, true, true)
            com.amap.api.location.AMapLocationClient.updatePrivacyAgree(this, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, foodFragment, "4").hide(foodFragment)
                add(R.id.fragment_container, mapFragment, "3").hide(mapFragment)
                add(R.id.fragment_container, trainerFragment, "2").hide(trainerFragment)
                add(R.id.fragment_container, homeFragment, "1").commit()
            }
        }

        setupCustomBottomNav()
    }

    private fun setupCustomBottomNav() {
        val tabHome = findViewById<View>(R.id.tab_home)
        val tabTrainer = findViewById<View>(R.id.tab_trainer)
        val tabMap = findViewById<View>(R.id.tab_map)
        val tabFood = findViewById<View>(R.id.tab_food)

        tabHome.setOnClickListener {
            selectTab(0)
            switchFragment(homeFragment)
        }
        tabTrainer.setOnClickListener {
            selectTab(1)
            switchFragment(trainerFragment)
        }
        tabMap.setOnClickListener {
            selectTab(2)
            switchFragment(mapFragment)
        }
        tabFood.setOnClickListener {
            selectTab(3)
            switchFragment(foodFragment)
        }

        // Activate the first tab by default
        selectTab(0)
    }

    private fun selectTab(index: Int) {
        val icons = listOf(R.id.icon_home, R.id.icon_trainer, R.id.icon_map, R.id.icon_food)
        val bgs = listOf(R.id.bg_home, R.id.bg_trainer, R.id.bg_map, R.id.bg_food)
        val texts = listOf(R.id.text_home, R.id.text_trainer, R.id.text_map, R.id.text_food)

        for (i in icons.indices) {
            val icon = findViewById<ImageView>(icons[i])
            val bg = findViewById<View>(bgs[i])
            val text = findViewById<TextView>(texts[i])

            if (i == index) {
                // The icon and the circle translate upwards (-50f)
                bg.animate().alpha(1f).translationY(-50f).setDuration(300).start()
                icon.animate().translationY(-50f).setDuration(300).start()
                text.setTextColor(android.graphics.Color.parseColor("#06B6D4")) // The text lights up with neon color
            } else {
                // The icon returns to its bottom position
                bg.animate().alpha(0f).translationY(0f).setDuration(300).start()
                icon.animate().translationY(0f).setDuration(300).start()
                text.setTextColor(android.graphics.Color.parseColor("#FFFFFF")) // The text returns to white
            }
        }
    }

    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment != targetFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit()
            activeFragment = targetFragment
        }
    }
}