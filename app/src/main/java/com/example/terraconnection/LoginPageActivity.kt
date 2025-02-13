package com.example.terraconnection // ✅ This must match exactly

import LoginViewModel
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import com.example.terraconnection.databinding.ActivityLoginPageBinding

class LoginPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPageBinding
    private val viewModel: LoginViewModel by viewModels() // ✅ Initialize ViewModel

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
                viewModel.login(username, password) // ✅ Now viewModel is defined
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loginResult.observe(this, Observer { result ->
            result.onSuccess { response ->
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_LONG).show()
                val intent = Intent(this, HomePanelActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }.onFailure { error ->
                Toast.makeText(this, "Login Failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
