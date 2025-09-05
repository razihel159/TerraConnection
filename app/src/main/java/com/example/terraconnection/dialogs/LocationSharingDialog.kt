package com.example.terraconnection.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.activities.ClassLocationActivity
import com.example.terraconnection.services.LocationService

class LocationSharingDialog : DialogFragment() {
    private var classId: String? = null
    private var className: String? = null
    private var isLocationServiceRunning = false

    companion object {
        fun newInstance(classId: String, className: String): LocationSharingDialog {
            return LocationSharingDialog().apply {
                arguments = Bundle().apply {
                    putString("classId", classId)
                    putString("className", className)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        classId = arguments?.getString("classId")
        className = arguments?.getString("className")
        isLocationServiceRunning = classId?.let { 
            LocationService.isSharing(requireContext(), it)
        } ?: false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_location_sharing, null)

            val titleText = view.findViewById<TextView>(R.id.titleText)
            val messageText = view.findViewById<TextView>(R.id.messageText)
            val startSharingButton = view.findViewById<Button>(R.id.startSharingButton)
            val viewLocationsButton = view.findViewById<Button>(R.id.viewLocationsButton)
            val stopSharingButton = view.findViewById<Button>(R.id.stopSharingButton)

            titleText.text = className
            
            val role = SessionManager.getRole(activity)
            
            if (role == "professor") {
                // For professors, focus on viewing student locations
                messageText.text = "View student locations in this class"
                startSharingButton.visibility = android.view.View.GONE
                stopSharingButton.visibility = android.view.View.GONE
            } else {
                // For students, show sharing options
                messageText.text = if (isLocationServiceRunning) {
                    "You are currently sharing your location with this class"
                } else {
                    "Would you like to share your location with this class?"
                }
                
                // Show/hide buttons based on current state
                startSharingButton.visibility = if (isLocationServiceRunning) android.view.View.GONE else android.view.View.VISIBLE
                stopSharingButton.visibility = if (isLocationServiceRunning) android.view.View.VISIBLE else android.view.View.GONE
            }

            startSharingButton.setOnClickListener {
                startLocationSharing()
                dismiss()
            }

            viewLocationsButton.setOnClickListener {
                startClassLocationActivity()
                dismiss()
            }

            stopSharingButton.setOnClickListener {
                stopLocationSharing()
                dismiss()
            }

            builder.setView(view)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun startLocationSharing() {
        val serviceIntent = Intent(requireContext(), LocationService::class.java).apply {
            putExtra("classId", classId)
            putExtra("className", className)
        }
        requireContext().startService(serviceIntent)
    }

    private fun stopLocationSharing() {
        requireContext().stopService(Intent(requireContext(), LocationService::class.java))
        LocationService.clearSharingState(requireContext())
        dismiss()
    }

    private fun startClassLocationActivity() {
        val intent = Intent(requireContext(), ClassLocationActivity::class.java).apply {
            putExtra("classId", classId)
            putExtra("className", className)
        }
        startActivity(intent)
    }
} 