package com.example.terraconnection.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.services.GlobalLocationService

class LocationSettingsFragment : Fragment() {
    
    // Single toggle for professors
    private lateinit var locationToggle: Switch
    
    // Dual toggles for students
    private lateinit var classesToggle: Switch
    private lateinit var guardianToggle: Switch
    
    private lateinit var statusText: TextView
    private lateinit var descriptionText: TextView
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        locationToggle = view.findViewById(R.id.locationToggle)
        classesToggle = view.findViewById(R.id.classesToggle)
        guardianToggle = view.findViewById(R.id.guardianToggle)
        statusText = view.findViewById(R.id.statusText)
        descriptionText = view.findViewById(R.id.descriptionText)
        
        setupToggles()
        updateUI()
    }
    
    private fun setupToggles() {
        val role = SessionManager.getRole(requireContext())
        
        // This fragment is only used by students now
        // Professors use MapsFragment, guardians have no location tab
        if (role == "student") {
            // Show dual toggles for students
            classesToggle.setOnCheckedChangeListener { _, isChecked ->
                updateStudentClassesSharing(isChecked)
            }
            
            guardianToggle.setOnCheckedChangeListener { _, isChecked ->
                updateStudentGuardianSharing(isChecked)
            }
        }
    }
    
    private fun startLocationSharing() {
        // Check permissions first
        if (!hasLocationPermission()) {
            requestLocationPermission()
            locationToggle.isChecked = false
            return
        }
        
        val intent = Intent(requireContext(), GlobalLocationService::class.java)
        requireContext().startService(intent)
        
        updateUI()
        Toast.makeText(
            requireContext(), 
            "Location sharing enabled for all your classes", 
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun stopLocationSharing() {
        val intent = Intent(requireContext(), GlobalLocationService::class.java)
        requireContext().stopService(intent)
        
        updateUI()
        Toast.makeText(
            requireContext(), 
            "Location sharing disabled", 
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun updateUI() {
        val role = SessionManager.getRole(requireContext())
        
        // This fragment is only used by students now
        if (role == "student") {
            // Show dual toggles for students
            val shareWithClasses = getStudentClassesSharing()
            val shareWithGuardian = getStudentGuardianSharing()
            
            classesToggle.isChecked = shareWithClasses
            guardianToggle.isChecked = shareWithGuardian
            
            // Update status based on what's enabled
            when {
                shareWithClasses && shareWithGuardian -> {
                    statusText.text = "Location Sharing: ON"
                    statusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                    descriptionText.text = "Your general location is being shared with professors and your guardian"
                }
                shareWithClasses -> {
                    statusText.text = "Location Sharing: PARTIAL"
                    statusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
                    descriptionText.text = "Your general location is being shared with professors only"
                }
                shareWithGuardian -> {
                    statusText.text = "Location Sharing: PARTIAL"
                    statusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
                    descriptionText.text = "Your general location is being shared with your guardian only"
                }
                else -> {
                    statusText.text = "Location Sharing: OFF"
                    statusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    descriptionText.text = "Enable toggles above to share your general location"
                }
            }
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationSharing()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission is required to share your location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun updateStudentClassesSharing(isEnabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("StudentLocationPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("share_with_classes", isEnabled).apply()
        
        updateLocationService()
        updateUI()
        
        Toast.makeText(
            requireContext(),
            if (isEnabled) "Sharing with classes enabled" else "Sharing with classes disabled",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun updateStudentGuardianSharing(isEnabled: Boolean) {
        val prefs = requireContext().getSharedPreferences("StudentLocationPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("share_with_guardian", isEnabled).apply()
        
        updateLocationService()
        updateUI()
        
        Toast.makeText(
            requireContext(),
            if (isEnabled) "Sharing with guardian enabled" else "Sharing with guardian disabled",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun updateLocationService() {
        val prefs = requireContext().getSharedPreferences("StudentLocationPrefs", Context.MODE_PRIVATE)
        val shareWithClasses = prefs.getBoolean("share_with_classes", true)
        val shareWithGuardian = prefs.getBoolean("share_with_guardian", true)
        
        // Start service if either sharing option is enabled
        if (shareWithClasses || shareWithGuardian) {
            if (!hasLocationPermission()) {
                requestLocationPermission()
                return
            }
            val intent = Intent(requireContext(), GlobalLocationService::class.java)
            requireContext().startService(intent)
        } else {
            // Stop service if both are disabled
            val intent = Intent(requireContext(), GlobalLocationService::class.java)
            requireContext().stopService(intent)
        }
    }
    
    private fun getStudentClassesSharing(): Boolean {
        val prefs = requireContext().getSharedPreferences("StudentLocationPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("share_with_classes", true)
    }
    
    private fun getStudentGuardianSharing(): Boolean {
        val prefs = requireContext().getSharedPreferences("StudentLocationPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("share_with_guardian", true)
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
}
