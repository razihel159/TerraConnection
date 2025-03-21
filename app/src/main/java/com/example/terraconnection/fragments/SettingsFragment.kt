package com.example.terraconnection.fragments

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.activities.LoginPageActivity
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.User
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.terraconnection.databinding.FragmentSettingsBinding
import androidx.activity.result.ActivityResultLauncher

typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

class SettingsFragment : Fragment() {
    private lateinit var prefs: SharedPreferences
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var getContent: ActivityResultLauncher<String>
    private lateinit var cropImage: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Register activity result launchers
        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedImageUri ->
                startCrop(selectedImageUri)
            }
        }

        cropImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == -1) { // RESULT_OK
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let { uri ->
                    uploadProfilePicture(uri)
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(requireContext(), "Error cropping image: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        setupUI(view)
        fetchUsers(view)

        // Setup profile picture button
        view.findViewById<Button>(R.id.btnChangeProfilePic).setOnClickListener {
            getContent.launch("image/*")
        }
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

    private fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        
        val uCrop = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(500, 500)

        // Customize UCrop appearance
        uCrop.withOptions(UCrop.Options().apply {
            setCompressionQuality(80)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false)
            setStatusBarColor(resources.getColor(R.color.violet, null))
            setToolbarColor(resources.getColor(R.color.violet, null))
            setToolbarTitle("Crop Profile Picture")
        })

        cropImage.launch(uCrop.getIntent(requireContext()))
    }

    private fun uploadProfilePicture(imageUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            var inputStream: java.io.InputStream? = null
            var outputStream: FileOutputStream? = null
            var parcelFileDescriptor: android.os.ParcelFileDescriptor? = null
            val file = File(requireContext().cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
            
            try {
                // Convert Uri to File
                parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(imageUri, "r")
                inputStream = requireContext().contentResolver.openInputStream(imageUri)
                outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                
                // Ensure streams are closed before proceeding with upload
                inputStream?.close()
                outputStream?.close()
                parcelFileDescriptor?.close()
                inputStream = null
                outputStream = null
                parcelFileDescriptor = null

                // Create MultipartBody.Part
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("profile_picture", file.name, requestBody)

                // Get token
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Make API call
                val response = RetrofitClient.apiService.updateProfilePicture("Bearer $token", part)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val profilePicPath = response.body()?.profile_picture
                        if (profilePicPath != null) {
                            // Update profile picture in UI
                            val profilePicUrl = if (profilePicPath.startsWith("/")) {
                                RetrofitClient.BASE_URL.removeSuffix("/") + profilePicPath
                            } else {
                                RetrofitClient.BASE_URL.removeSuffix("/") + "/" + profilePicPath
                            }
                            Glide.with(requireContext())
                                .load(profilePicUrl)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(binding.profileImageView)
                            
                            Toast.makeText(requireContext(), "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ProfilePicture", "Error uploading profile picture: ${e.message}")
                    Toast.makeText(requireContext(), "Error uploading profile picture", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // Ensure all resources are closed
                try {
                    inputStream?.close()
                    outputStream?.close()
                    parcelFileDescriptor?.close()
                } catch (e: Exception) {
                    Log.e("ProfilePicture", "Error closing resources: ${e.message}")
                }
                
                // Clean up the temporary file
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("ProfilePicture", "Error deleting temporary file: ${e.message}")
                }
            }
        }
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
                            
                            // Load profile picture if available
                            if (!user.profile_picture.isNullOrEmpty()) {
                                val profilePicUrl = if (user.profile_picture.startsWith("/")) {
                                    RetrofitClient.BASE_URL.removeSuffix("/") + user.profile_picture
                                } else {
                                    RetrofitClient.BASE_URL.removeSuffix("/") + "/" + user.profile_picture
                                }
                                Glide.with(requireContext())
                                    .load(profilePicUrl)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(binding.profileImageView)
                            } else {
                                Glide.with(requireContext())
                                    .load(R.drawable.default_profile)
                                    .into(binding.profileImageView)
                            }
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

