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
import com.example.terraconnection.activities.StudentProfile
import com.example.terraconnection.adapters.ScheduleAdapter
import com.example.terraconnection.adapters.OnScheduleClickListener
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.databinding.FragmentHomePanelBinding
import kotlinx.coroutines.launch
import android.util.Log
import com.example.terraconnection.activities.AttendanceLogs
import com.example.terraconnection.activities.ListStudentActivity

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

        val profileIconButton: ImageButton = binding.studentProfile

        // Apply window insets to the main layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Determine user role from session manager
        val role = SessionManager.getRole(requireContext())

        // Remove (hide) student status layout when role is "student"
        if (role == "student") {
            binding.studentStatus.visibility = View.GONE
        }

        if (role == "guardian") {
            binding.notifyButton.visibility = View.GONE
        }

        // Make Attendance Logs clickable to open another activity
        binding.attendanceHistory.setOnClickListener {
            val intent = Intent(requireContext(), AttendanceLogs::class.java)
            startActivity(intent)
        }

        // Set up RecyclerView and fetch user details
        lifecycleScope.launch {
            fetchUserDetails()
            setupRecyclerView()
        }

        // Profile icon button action
        profileIconButton.setOnClickListener {
            val intent = Intent(requireContext(), StudentProfile::class.java)
            startActivity(intent)
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
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                binding.subjectCard.adapter = adapter
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
        intent.putExtra("class_code", schedule.classCode)
        intent.putExtra("class_name", schedule.className)
        intent.putExtra("room", schedule.room)
        intent.putExtra("time", "${schedule.startTime} - ${schedule.endTime}")
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Retrieve notification from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)
        val notificationMessage = sharedPreferences.getString("notification_message", "No notifications yet.")
        // Update UI
        binding.studentNotification.text = notificationMessage
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
