package com.example.terraconnection.fragments

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.activities.LoginPageActivity
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

class SettingsFragment : Fragment() {
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        setupUI(view)
        fetchUsers(view)
    }

    private fun setupUI(view: View) {
        val role = SessionManager.getRole(requireContext())
        
        // Show notifications section only for students
        val notificationsCard = view.findViewById<View>(R.id.notificationsCard)
        notificationsCard.alpha = 0f
        notificationsCard.visibility = if (role == "student") View.VISIBLE else View.GONE
        
        if (role == "student") {
            ObjectAnimator.ofFloat(notificationsCard, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            
            // Setup Notification Switches
            view.findViewById<SwitchCompat>(R.id.switchClassReminders).apply {
                isChecked = prefs.getBoolean(PREF_CLASS_REMINDERS, true)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(PREF_CLASS_REMINDERS, isChecked).apply()
                    if (isChecked) {
                        Toast.makeText(context, "Class reminders enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Class reminders disabled", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            view.findViewById<SwitchCompat>(R.id.switchAttendanceUpdates).apply {
                isChecked = prefs.getBoolean(PREF_ATTENDANCE_UPDATES, true)
                setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean(PREF_ATTENDANCE_UPDATES, isChecked).apply()
                    if (isChecked) {
                        Toast.makeText(context, "Attendance updates enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Attendance updates disabled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Setup Dark Mode Switch with animation
        view.findViewById<SwitchCompat>(R.id.switchDarkMode).apply {
            alpha = 0f
            visibility = View.VISIBLE
            isChecked = prefs.getBoolean(PREF_DARK_MODE, false)
            ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(PREF_DARK_MODE, isChecked).apply()
                applyDarkMode(isChecked)
            }
        }

        // Setup Logout Button with animation
        view.findViewById<Button>(R.id.btnLogout).apply {
            alpha = 0f
            visibility = View.VISIBLE
            ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            setOnClickListener {
                showLogoutConfirmation()
            }
        }
    }

    private fun applyDarkMode(isDarkMode: Boolean) {
        val mode = if (isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
        requireActivity().recreate()
    }

    private fun fetchUsers(view: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token: String? = SessionManager.getToken(requireContext())
                Log.e("TOKEN", "Token being sent: $token")

                if (token.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                        logout()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getUsers("Bearer $token")

                if (response.isSuccessful) {
                    val user: User? = response.body()
                    if (user != null) {
                        withContext(Dispatchers.Main) {
                            view.findViewById<TextView>(R.id.firstNameText).text = "${user.first_name} ${user.last_name}"
                            view.findViewById<TextView>(R.id.emailText).text = user.email
                            view.findViewById<TextView>(R.id.studentIdText).text = user.school_id ?: "N/A"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Failed response: ${response.code()} - ${response.errorBody()?.string()}")
                    if (response.code() == 401) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                            logout()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to log out?")
        builder.setPositiveButton("No") { dialog, _ -> dialog.dismiss() }
        builder.setNegativeButton("Yes") { _, _ -> logout() }
        builder.show()
    }

    private fun logout() {
        // Clear all preferences when logging out
        prefs.edit().clear().apply()
        SessionManager.clearSession(requireContext())
        val intent = Intent(requireContext(), LoginPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    companion object {
        private const val PREF_CLASS_REMINDERS = "pref_class_reminders"
        private const val PREF_ATTENDANCE_UPDATES = "pref_attendance_updates"
        private const val PREF_DARK_MODE = "pref_dark_mode"
    }
}

