package com.example.terraconnection.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terraconnection.adapters.ScheduleAdapter
import com.example.terraconnection.adapters.OnScheduleClickListener
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.databinding.ActivityHomePanelBinding
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.terraconnection.api.ApiService
import com.example.terraconnection.api.RetrofitClient
import android.util.Log
import com.example.terraconnection.fragments.MapsFragment
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.fragments.CalendarProfFragment
import com.example.terraconnection.fragments.HomePanelFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomePanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_panel)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Load HomeFragment initially
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomePanelFragment())
            .commit()

        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.nav_home -> HomePanelFragment()
                R.id.nav_calendar -> CalendarProfFragment()
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
