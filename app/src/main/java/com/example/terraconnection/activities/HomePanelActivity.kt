package com.example.terraconnection.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.terraconnection.fragments.MapsFragment
import com.example.terraconnection.fragments.LocationSettingsFragment
import com.example.terraconnection.R
import com.example.terraconnection.ThemeManager
import com.example.terraconnection.SessionManager
import com.example.terraconnection.fragments.CalendarProfFragment
import com.example.terraconnection.fragments.CalendarStudFragment
import com.example.terraconnection.fragments.CalendarGuardFragment
import com.example.terraconnection.fragments.HomePanelFragment
import com.example.terraconnection.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import android.view.View
import com.example.terraconnection.SpotlightTourManager

class HomePanelActivity : AppCompatActivity() {
    private val fragments = mutableMapOf<Int, Fragment>()
    
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions granted
        } else {
            showPermissionRationaleDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before calling super.onCreate and setContentView
        ThemeManager.applyTheme(this)
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_panel)

        // Request permissions
        requestPermissions()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Get user role
        val role = SessionManager.getRole(this)

        // Initialize fragments
        fragments[R.id.nav_home] = HomePanelFragment()
        fragments[R.id.nav_calendar] = when (role) {
            "professor" -> CalendarProfFragment()
            "guardian" -> CalendarGuardFragment()
            else -> CalendarStudFragment()
        }
        fragments[R.id.nav_profile] = SettingsFragment()
        
        // Different location fragments based on role
        when (role) {
            "student" -> {
                // Students get dual toggle interface
                fragments[R.id.nav_location] = LocationSettingsFragment()
            }
            "professor" -> {
                // Professors get class list to view student locations
                fragments[R.id.nav_location] = MapsFragment()
            }
            // Guardians get no location tab (handled below)
        }
        
        // Hide location tab for guardians
        if (role == "guardian") {
            bottomNav.menu.findItem(R.id.nav_location).isVisible = false
        }

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

        // Check if we should show the spotlight tour
        if (SpotlightTourManager.shouldShowTour(this)) {
            // Wait for views to be laid out
            bottomNav.post {
                showSpotlightTour(role ?: "student", bottomNav)
            }
        }
    }

    private fun requestPermissions() {
        val hasPermissions = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location permission to function properly. Please grant the permission.")
            .setPositiveButton("Grant") { _, _ ->
                permissionLauncher.launch(requiredPermissions)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .create()
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Location permission is required for this app. Please enable it in settings.")
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .create()
            .show()
    }

    private fun showSpotlightTour(role: String, bottomNav: BottomNavigationView) {
        val views = mutableMapOf<String, View>()

        // Common views
        views["bottomNavCalendar"] = bottomNav.findViewById(R.id.nav_calendar)
        views["bottomNavLocation"] = bottomNav.findViewById(R.id.nav_location)

        // Get the current fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is HomePanelFragment) {
            val fragmentView = currentFragment.view
            // Role-specific views
            when (role) {
                "student" -> {
                    fragmentView?.findViewById<View>(R.id.subjectNotification)?.let { views["subjectNotification"] = it }
                    fragmentView?.findViewById<View>(R.id.attendanceLog)?.let { views["attendanceLog"] = it }
                }
                "professor" -> {
                    fragmentView?.findViewById<View>(R.id.subjectCard)?.let { views["scheduleSection"] = it }
                }
                "guardian" -> {
                    fragmentView?.findViewById<View>(R.id.studentStatusContainer)?.let { views["studentStatusContainer"] = it }
                }
            }
        }

        SpotlightTourManager.startTour(this, role, views)
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment !is HomePanelFragment) {
            // If not on home fragment, navigate to home
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragments[R.id.nav_home]!!)
                .commit()
            findViewById<BottomNavigationView>(R.id.bottomNavigation).selectedItemId = R.id.nav_home
        } else {
            // If on home fragment, perform default back behavior
            super.onBackPressed()
        }
    }
}
