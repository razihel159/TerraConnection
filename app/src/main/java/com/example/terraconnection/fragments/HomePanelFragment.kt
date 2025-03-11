package com.example.terraconnection.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.adapters.ScheduleAdapter
import com.example.terraconnection.adapters.OnScheduleClickListener
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.databinding.FragmentHomePanelBinding
import kotlinx.coroutines.launch
import android.util.Log
import com.example.terraconnection.activities.AttendanceLogs
import com.example.terraconnection.activities.ListStudentActivity
import com.example.terraconnection.activities.NotificationActivity
import java.util.*

class HomePanelFragment : Fragment(R.layout.fragment_home_panel), OnScheduleClickListener {

    private var _binding: FragmentHomePanelBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomePanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val role = SessionManager.getRole(requireContext())

        if (role == "student") {
            binding.studentStatus.visibility = View.GONE
        }

        if (role == "guardian") {
            binding.notifyButton.visibility = View.GONE
        }
        if (role == "professor") {
            binding.attendanceLog.visibility = View.GONE
            binding.studentStatus.visibility = View.GONE
        }

        if (role == "student" || role == "professor") {
            binding.attendanceHistory.setOnClickListener {
                val intent = Intent(requireContext(), AttendanceLogs::class.java)
                startActivity(intent)
            }
        }

        binding.subjectNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }


        lifecycleScope.launch {
            fetchUserDetails()
            setupRecyclerView()
        }
    }

    private suspend fun fetchUserDetails() {
        try {
            val token = SessionManager.getToken(requireContext())?.let { "Bearer $it" }
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
            val token = SessionManager.getToken(requireContext())?.let { "Bearer $it" }
                ?: throw Exception("No authentication token found")
            val role = SessionManager.getRole(requireContext())

            // Hide notification section for professor role
            if (role == "professor") {
                binding.subjectNotification.visibility = View.GONE
            }

            val scheduleResponse = when (role) {
                "professor" -> apiService.getProfessorSchedule(token)
                else -> apiService.getStudentSchedule(token)
            }

            if (scheduleResponse.isSuccessful) {
                val allSchedules: List<Schedule> = scheduleResponse.body()?.schedule ?: emptyList()
                
                // Filter schedules for today
                val calendar = Calendar.getInstance()
                val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
                
                val todaySchedules = allSchedules.filter { schedule ->
                    schedule.schedule?.split(",")?.map { it.trim() }?.contains(dayOfWeek) == true
                }
                
                Log.d("Schedule", "Today's schedules: $todaySchedules")
                
                if (todaySchedules.isNotEmpty()) {
                    val adapter = ScheduleAdapter(todaySchedules.toMutableList(), this)
                    binding.subjectCard.layoutManager = LinearLayoutManager(
                        requireContext(),
                        LinearLayoutManager.HORIZONTAL,
                        false
                    )
                    binding.subjectCard.adapter = adapter
                } else {
                    // Show a message when there are no classes today
                    Toast.makeText(
                        requireContext(),
                        "No classes scheduled for today",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to load schedule: ${scheduleResponse.message()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } finally {
            binding.loadingIndicator.visibility = View.GONE
        }
    }

    override fun onScheduleClick(schedule: Schedule) {
        val intent = Intent(requireContext(), ListStudentActivity::class.java)
        intent.putExtra("classId", schedule.id.toString())
        intent.putExtra("classCode", schedule.class_code)
        intent.putExtra("className", schedule.class_name)
        intent.putExtra("room", schedule.room)
        intent.putExtra("time", "${schedule.start_time} - ${schedule.end_time}")
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Retrieve notification from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val notificationMessage = sharedPreferences.getString("notification_message", "No notifications yet.")
        // Update UI
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
