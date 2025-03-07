package com.example.terraconnection.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.terraconnection.fragments.MapsFragment
import com.example.terraconnection.R
import com.example.terraconnection.fragments.CalendarProfFragment
import com.example.terraconnection.fragments.HomePanelFragment
import com.example.terraconnection.fragments.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomePanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_panel)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomePanelFragment())
            .commit()

        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomePanelFragment()
                R.id.nav_calendar -> CalendarProfFragment()
                R.id.nav_profile -> ProfileFragment()
                R.id.nav_location -> MapsFragment()
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, it)
                    .commit()
                true
            } ?: false
        }
    }
}
