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
import com.example.terraconnection.fragments.HomePanelFragment
import com.example.terraconnection.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment

class HomePanelActivity : AppCompatActivity() {
    private val fragments = mutableMapOf<Int, Fragment>()
    
    private val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys
        
        if (deniedPermissions.isNotEmpty()) {
            // Check if we should show rationale for any permission
            val shouldShowRationale = deniedPermissions.any {
                shouldShowRequestPermissionRationale(it)
            }

            if (shouldShowRationale) {
                showPermissionRationaleDialog(deniedPermissions.toList())
            } else {
                // User has selected "Don't ask again", direct them to settings
                showSettingsDialog()
            }
        }

        // Special handling for notification permission
        permissions[Manifest.permission.POST_NOTIFICATIONS]?.let { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "Notification permission denied. You won't receive class notifications.",
                    Toast.LENGTH_LONG
                ).show()
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
        fragments[R.id.nav_calendar] = if (role == "professor") CalendarProfFragment() else CalendarStudFragment()
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

    private fun showPermissionRationaleDialog(permissions: List<String>) {
        val message = buildString {
            append("The following permissions are required:\n\n")
            permissions.forEach { permission ->
                append("• ${getPermissionReason(permission)}\n")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                permissionLauncher.launch(permissions.toTypedArray())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Some permissions are required for the app to function properly. Please grant them in Settings.")
            .setPositiveButton("Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun getPermissionReason(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> "Location access to share your position"
            Manifest.permission.FOREGROUND_SERVICE_LOCATION -> "Background location access for continuous updates"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications to receive important class updates and reminders"
            else -> "Required for app functionality"
        }
    }
}
