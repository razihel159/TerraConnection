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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.terraconnection.api.ApiService
import com.example.terraconnection.api.RetrofitClient
import android.util.Log

class HomePanelActivity : AppCompatActivity(), OnScheduleClickListener {
    private lateinit var binding: ActivityHomePanelBinding
    
    private val apiService: ApiService = RetrofitClient.apiService

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
        lifecycleScope.launch {
            fetchUserDetails()
            setupRecyclerView()
        }

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

    private suspend fun fetchUserDetails() {
        try {
            val token = SessionManager.getToken(this)?.let { "Bearer $it" }
                ?: throw Exception("No authentication token found")

            val response = apiService.getUsers(token)

            if (response.isSuccessful) {
                val user = response.body()
                if (user != null) {
                    val fullName = "${user.first_name} ${user.last_name}"
                    binding.loginGreeting.text = "Hello $fullName"
                }
            } else {
                Log.e("User Fetch", "Failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("User Fetch", "Error: ${e.message}")
        }
    }


    private suspend fun setupRecyclerView() {
        try {
            binding.loadingIndicator.visibility = View.VISIBLE

            val token = SessionManager.getToken(this)?.let { "Bearer $it" }
                ?: throw Exception("No authentication token found")
            
            val role = SessionManager.getRole(this)
            
            val scheduleResponse = when (role) {
                "professor" -> apiService.getProfessorSchedule(token)
                else -> apiService.getStudentSchedule(token)
            }

            if (scheduleResponse.isSuccessful) {
                val scheduleList: List<Schedule> = scheduleResponse.body()?.schedule ?: emptyList()
                Log.d("Schedule", "Received schedules: $scheduleList")
                
                if (scheduleList.isNotEmpty()) {
                    val firstSchedule = scheduleList[0]
                    Log.d("Schedule", """
                        First schedule details:
                        Class Code: ${firstSchedule.classCode}
                        Class Name: ${firstSchedule.className}
                        Room: ${firstSchedule.room}
                        Time: ${firstSchedule.startTime} - ${firstSchedule.endTime}
                    """.trimIndent())
                }

                val adapter = ScheduleAdapter(scheduleList, this)
                binding.subjectCard.layoutManager = LinearLayoutManager(
                    this,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                binding.subjectCard.adapter = adapter
            } else {
                Toast.makeText(
                    this,
                    "Failed to load schedule: ${scheduleResponse.message()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            binding.loadingIndicator.visibility = View.GONE
        }
    }

    override fun onScheduleClick(schedule: Schedule) {
        val intent = Intent(this, ListStudentActivity::class.java)
        intent.putExtra("class_code", schedule.classCode)
        intent.putExtra("class_name", schedule.className)
        intent.putExtra("room", schedule.room)
        intent.putExtra("time", "${schedule.startTime} - ${schedule.endTime}")
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
