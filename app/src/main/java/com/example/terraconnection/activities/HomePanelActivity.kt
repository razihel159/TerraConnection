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
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionRationaleDialog()
            } else {
                showSettingsDialog()
            }
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

    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
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
}
