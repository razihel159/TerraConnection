package com.example.terraconnection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.terraconnection.databinding.ActivityHomePanelBinding

class HomePanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomePanelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomePanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val profileIconButton: ImageButton = binding.studentProfile

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.subjectCard.setOnClickListener {
            val intent = Intent(this, ListStudentActivity::class.java)
            startActivity(intent)
        }

        profileIconButton.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        binding.calendarHistory.setOnClickListener {
            val intent = Intent(this, AttendanceLogs::class.java)
            startActivity(intent)
        }
        binding.gpsLocation.setOnClickListener {
            val intent = Intent(this, MapsFragment::class.java)
            startActivity(intent)
        }

    }
    override fun onResume() {
        super.onResume()
        // Retrieve notification from SharedPreferences
        val sharedPreferences = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val notificationMessage = sharedPreferences.getString("notification_message", "No notifications yet.")

        // Update UI
        binding.studentNotification.text = notificationMessage
    }
}