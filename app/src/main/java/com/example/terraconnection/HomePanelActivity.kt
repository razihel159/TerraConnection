package com.example.terraconnection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terraconnection.adapters.ScheduleAdapter
import com.example.terraconnection.adapters.OnScheduleClickListener
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.databinding.ActivityHomePanelBinding

class HomePanelActivity : AppCompatActivity(), OnScheduleClickListener {
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

        // ✅ Set up RecyclerView
        setupRecyclerView()

        profileIconButton.setOnClickListener {
            val intent = Intent(this, StudentProfile::class.java)
            startActivity(intent)
        }

        binding.calendarHistory.setOnClickListener {
            val intent = Intent(this, CalendarProf::class.java)
            startActivity(intent)
        }

        binding.gpsLocation.setOnClickListener {
            val intent = Intent(this, MapsFragment::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        val scheduleList = listOf(
            Schedule("ITE 384", "Computer Forensics", "ITS-200", "07:30 AM-09:00 AM"),
            Schedule("ITE 385", "Ethical Hacking", "ITS-200", "12:00 PM - 01:30 PM"),
        )

        val adapter = ScheduleAdapter(scheduleList, this) // Pass this as the listener

        binding.subjectCard.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.subjectCard.adapter = adapter
    }

    override fun onScheduleClick(schedule: Schedule) {

        val intent = Intent(this, ListStudentActivity::class.java)
        intent.putExtra("subject_code", schedule.subjectCode)
        intent.putExtra("subject_name", schedule.subjectName)
        intent.putExtra("room", schedule.room)
        intent.putExtra("time", schedule.time)
        startActivity(intent)
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
