package com.example.terraconnection.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.terraconnection.fragments.MapsFragment
import com.example.terraconnection.R
import com.example.terraconnection.ThemeManager
import com.example.terraconnection.fragments.CalendarProfFragment
import com.example.terraconnection.fragments.HomePanelFragment
import com.example.terraconnection.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment

class HomePanelActivity : AppCompatActivity() {
    private val fragments = mutableMapOf<Int, Fragment>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before calling super.onCreate and setContentView
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_panel)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Initialize fragments
        fragments[R.id.nav_home] = HomePanelFragment()
        fragments[R.id.nav_calendar] = CalendarProfFragment()
        fragments[R.id.nav_profile] = SettingsFragment()
        fragments[R.id.nav_location] = MapsFragment()

        // Set initial fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragments[R.id.nav_home]!!)
            .commit()

        bottomNav.setOnItemSelectedListener { item ->
            fragments[item.itemId]?.let { fragment ->
                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.fragmentContainer, fragment)
                    .commit()
                true
            } ?: false
        }
    }
}
