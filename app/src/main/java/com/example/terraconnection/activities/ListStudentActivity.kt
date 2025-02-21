package com.example.terraconnection.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terraconnection.adapters.StudentAdapter
import com.example.terraconnection.data.Student
import com.example.terraconnection.databinding.ActivityListStudentBinding

class ListStudentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListStudentBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var studentAdapter: StudentAdapter
    private var studentList: List<Student> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)

        // Initialize RecyclerView
        binding.studentList.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(studentList) { studentName ->
            sendNotification(studentName)
        }
        binding.studentList.adapter = studentAdapter

        // Retrieve student data from intent
        studentList = intent.getParcelableArrayListExtra("studentList") ?: emptyList()
        studentAdapter.updateList(studentList)
    }

    private fun sendNotification(studentName: String) {
        val message = "Reminder: $studentName, please attend your class!"
        sharedPreferences.edit().putString("notification_message", message).apply()
        Toast.makeText(this, "Notification sent!", Toast.LENGTH_SHORT).show()
    }
}