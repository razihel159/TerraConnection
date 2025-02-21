package com.example.terraconnection.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.terraconnection.databinding.ActivityListStudentBinding

class ListStudentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListStudentBinding
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)

        binding.studentList.adapter = StudentAdapter { studentName ->
            sendNotification(studentName)
        }
    }

    private fun sendNotification(studentName: String) {
        val message = "Reminder: $studentName, please attend your class!"
        sharedPreferences.edit().putString("notification_message", message).apply()
        Toast.makeText(this, "Notification sent!", Toast.LENGTH_SHORT).show()
    }
}