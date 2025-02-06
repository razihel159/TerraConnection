package com.example.terraconnection

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.terraconnection.databinding.ActivityLoginChoiceBinding


class LoginChoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginChoiceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginChoiceBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.buttonStudent.setOnClickListener {
            val intent = Intent(this, LoginPageActivity::class.java)
            startActivity(intent)
        }

    }
}