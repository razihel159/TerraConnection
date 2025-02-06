package com.example.terraconnection

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.terraconnection.databinding.ActivityLoginPageBinding

class LoginPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Correct ViewBinding initialization
        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Handle system window insets

        // Fix Button Click Listener
        binding.btnSignUP.setOnClickListener {
            val intent = Intent(this, HomePanelActivity::class.java)
            startActivity(intent)
        }
    }
}
