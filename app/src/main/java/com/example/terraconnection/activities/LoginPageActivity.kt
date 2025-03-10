package com.example.terraconnection.activities

import LoginViewModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import com.example.terraconnection.SessionManager
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.FcmTokenRequest
import com.example.terraconnection.databinding.ActivityLoginPageBinding
import kotlin.random.Random
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPageBinding
    private val viewModel: LoginViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            Log.d("Notifications", "Permission granted")
        } else {
            // Permission denied
            Toast.makeText(this, 
                "Notification permission denied. You won't receive class notifications.", 
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check for existing token and auto-login if present
        val existingToken = SessionManager.getToken(this)
        if (!existingToken.isNullOrEmpty()) {
            // Verify token validity by making an API call
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.apiService.getMe("Bearer $existingToken")
                    if (response.isSuccessful) {
                        // Token is valid, proceed to home screen
                        val intent = Intent(this@LoginPageActivity, HomePanelActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                        return@launch
                    } else {
                        // Token is invalid, clear it
                        SessionManager.clearSession(this@LoginPageActivity)
                    }
                } catch (e: Exception) {
                    // Error occurred, clear token
                    SessionManager.clearSession(this@LoginPageActivity)
                }
            }
        }

        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale if needed
                    showNotificationPermissionRationale()
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        binding.btnSignIN.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                showCaptchaDialog(username, password) // Show custom CAPTCHA before login
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loginResult.observe(this, Observer { result ->
            result.onSuccess { response ->
                val token = response.token
                if (!token.isNullOrEmpty()) {
                    SessionManager.saveToken(this, token)
                    // Refresh FCM token after successful login
                    refreshFCMToken(token)
                }

                Toast.makeText(this, "Login Successful!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, HomePanelActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }.onFailure { error ->
                Toast.makeText(this, "Login Failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showCaptchaDialog(username: String, password: String) {
        val generatedCaptcha = generateCaptcha()
        val editText = EditText(this)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Captcha Verification")
            .setMessage("Enter the text: $generatedCaptcha")
            .setView(editText)
            .setPositiveButton("Verify") { _, _ ->
                val userInput = editText.text.toString().trim()
                if (userInput == generatedCaptcha) {
                    Toast.makeText(this, "CAPTCHA verified!", Toast.LENGTH_SHORT).show()
                    viewModel.login(username, password) // ✅ Proceed with login
                } else {
                    Toast.makeText(this, "CAPTCHA incorrect. Try again!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun generateCaptcha(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission")
            .setMessage("Notifications are required to receive important class updates and reminders. Please allow notifications to get the best experience.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun refreshFCMToken(authToken: String) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fcmToken = task.result
                    Log.d("Firebase", "FCM Token on login: $fcmToken")
                    sendTokenToServer(fcmToken, authToken)
                } else {
                    Log.e("Firebase", "Failed to get FCM token on login", task.exception)
                }
            }
    }

    private fun sendTokenToServer(fcmToken: String, authToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("Firebase", "Sending FCM token to server after login...")
                Log.d("Firebase", "Auth token being used: ${authToken.take(20)}...")
                Log.d("Firebase", "FCM token being sent: ${fcmToken.take(20)}...")
                
                val response = RetrofitClient.apiService.updateFcmToken(
                    "Bearer $authToken",
                    FcmTokenRequest(fcmToken)
                )

                if (response.isSuccessful) {
                    Log.d("Firebase", "Successfully updated FCM token on server after login")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("Firebase", "Failed to update token after login: ${response.code()} - ${response.message()}")
                    Log.e("Firebase", "Error body: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("Firebase", "Error updating token after login: ${e.message}", e)
            }
        }
    }
}
