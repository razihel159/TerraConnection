package com.example.terraconnection.activities

import LoginViewModel
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import com.example.terraconnection.SessionManager
import com.example.terraconnection.databinding.ActivityLoginPageBinding
import kotlin.random.Random

class LoginPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPageBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
}
