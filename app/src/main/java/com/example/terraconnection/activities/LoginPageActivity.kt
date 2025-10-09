package com.example.terraconnection.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.ThemeManager
import com.example.terraconnection.api.LoginViewModel
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.FcmTokenRequest
import com.example.terraconnection.databinding.ActivityLoginPageBinding
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.terraconnection.utils.CodeInputHelper
import com.example.terraconnection.views.CaptchaView
import android.widget.ImageButton

class LoginPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPageBinding
    private val viewModel: LoginViewModel by viewModels()
    private var tempToken: String? = null
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        ThemeManager.applyTheme(this)
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

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.btnSignIN.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                showCaptchaDialog(username, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loginResult.observe(this, Observer { result ->
            loadingDialog?.dismiss()
            result.onSuccess { response ->
                if (response.otpRequired) {
                    tempToken = response.tempToken
                    showOtpVerificationDialog()
                } else {
                    // Handle admin login (no OTP required)
                    response.token?.let { token ->
                        handleSuccessfulLogin(token)
                    } ?: run {
                        Toast.makeText(this, "Login Failed: Invalid token received", Toast.LENGTH_LONG).show()
                    }
                }
            }.onFailure { error ->
                Toast.makeText(this, "Login Failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })

        viewModel.otpVerificationResult.observe(this, Observer { result ->
            result.onSuccess { response ->
                handleSuccessfulLogin(response.token)
            }.onFailure { error ->
                Toast.makeText(this, "OTP Verification Failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showLoadingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun showCaptchaDialog(username: String, password: String) {
        var currentCaptcha = generateCaptcha()
        val dialogView = layoutInflater.inflate(R.layout.dialog_captcha_verification, null)
        val captchaView = dialogView.findViewById<CaptchaView>(R.id.captchaView)
        val digits = listOf(
            dialogView.findViewById<EditText>(R.id.digit1),
            dialogView.findViewById<EditText>(R.id.digit2),
            dialogView.findViewById<EditText>(R.id.digit3),
            dialogView.findViewById<EditText>(R.id.digit4),
            dialogView.findViewById<EditText>(R.id.digit5),
            dialogView.findViewById<EditText>(R.id.digit6)
        )
        
        // Set input type for CAPTCHA
        digits.forEach { editText ->
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            editText.filters = arrayOf(InputFilter.LengthFilter(1))
        }

        val btnVerifyCaptcha = dialogView.findViewById<MaterialButton>(R.id.btnVerifyCaptcha)
        val codeInputHelper = CodeInputHelper(digits)

        captchaView.setCaptchaText(currentCaptcha)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnVerifyCaptcha.setOnClickListener {
            val userInput = codeInputHelper.getCode()
            if (userInput == currentCaptcha) {
                dialog.dismiss()
                showLoadingDialog()
                viewModel.login(username, password)
            } else {
                codeInputHelper.setError(true)
                codeInputHelper.clearCode()
                // Generate new CAPTCHA
                currentCaptcha = generateCaptcha()
                captchaView.setCaptchaText(currentCaptcha)
            }
        }

        dialog.show()
        digits.first().requestFocus()
    }

    private fun generateCaptcha(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    private fun showOtpVerificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_otp_verification, null)
        val digits = listOf(
            dialogView.findViewById<EditText>(R.id.digit1),
            dialogView.findViewById<EditText>(R.id.digit2),
            dialogView.findViewById<EditText>(R.id.digit3),
            dialogView.findViewById<EditText>(R.id.digit4),
            dialogView.findViewById<EditText>(R.id.digit5),
            dialogView.findViewById<EditText>(R.id.digit6)
        )

        // Set input type for OTP
        digits.forEach { editText ->
            editText.inputType = InputType.TYPE_CLASS_NUMBER
            editText.filters = arrayOf(InputFilter.LengthFilter(1))
        }

        val btnVerifyOtp = dialogView.findViewById<MaterialButton>(R.id.btnVerifyOtp)
        val btnReturn = dialogView.findViewById<ImageButton>(R.id.btnReturn)
        val codeInputHelper = CodeInputHelper(digits)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnReturn.setOnClickListener {
            dialog.dismiss()
            // Clear any stored temporary token
            tempToken = null
            // Clear the password field for security
            binding.etPassword.text?.clear()
        }

        btnVerifyOtp.setOnClickListener {
            val otp = codeInputHelper.getCode()
            if (otp.length == 6 && tempToken != null) {
                viewModel.verifyOtp(tempToken!!, otp)
                dialog.dismiss()
            } else {
                codeInputHelper.setError(true)
                Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
        digits.first().requestFocus()
    }

    private fun handleSuccessfulLogin(token: String) {
        SessionManager.saveToken(this, token)
        refreshFCMToken(token)
        Toast.makeText(this, "Login Successful!", Toast.LENGTH_LONG).show()
        val intent = Intent(this, HomePanelActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
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

    override fun onDestroy() {
        super.onDestroy()
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}
