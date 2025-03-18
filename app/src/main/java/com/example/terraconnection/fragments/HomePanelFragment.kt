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
import com.example.terraconnection.adapters.AttendanceLogAdapter
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.databinding.FragmentHomePanelBinding
import kotlinx.coroutines.launch
import android.util.Log
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import com.example.terraconnection.activities.AttendanceLogs
import com.example.terraconnection.activities.ListStudentActivity
import com.example.terraconnection.activities.NotificationActivity
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

class HomePanelFragment : Fragment(R.layout.fragment_home_panel), OnScheduleClickListener {

    private var _binding: FragmentHomePanelBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService
    private lateinit var attendanceAdapter: AttendanceLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        // Pre-set initial visibility to INVISIBLE instead of GONE to maintain layout
        binding.studentStatus.alpha = 0f
        binding.attendanceLog.alpha = 0f
        binding.notifyButton.alpha = 0f

        when (role) {
            "student" -> {
                binding.studentStatus.visibility = View.GONE
            }
            "guardian" -> {
                binding.notifyButton.visibility = View.GONE
            }
            "professor" -> {
                binding.attendanceLog.visibility = View.GONE
                binding.studentStatus.visibility = View.GONE
                binding.subjectNotification.visibility = View.GONE
            }
        }

        // Animate visible elements
        if (role != "student") {
            animateViewIn(binding.studentStatus)
        }
        if (role != "professor") {
            animateViewIn(binding.attendanceLog)
        }
        if (role != "guardian") {
            animateViewIn(binding.notifyButton)
        }

        binding.subjectNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        setupRecyclerViews()
        lifecycleScope.launch {
            setupRecyclerView()
        }
        fetchRecentAttendanceLogs()

        lifecycleScope.launch {
            fetchUserDetails()
            if (role != "professor") {
                fetchNotifications()
            }
        }
    }

    private fun animateViewIn(view: View) {
        if (view.visibility != View.GONE) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 300
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun setupRecyclerViews() {
        // Setup schedule RecyclerView
        binding.subjectCard.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        // Setup attendance logs RecyclerView
        binding.rvAttendanceLogs.layoutManager = LinearLayoutManager(requireContext())
        attendanceAdapter = AttendanceLogAdapter()
        binding.rvAttendanceLogs.adapter = attendanceAdapter
    }

    private fun fetchRecentAttendanceLogs() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val response = RetrofitClient.apiService.getStudentAttendance("Bearer $token", today)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val logs = response.body()?.attendance ?: emptyList()
                        Log.d("HomePanelFragment", "Fetched attendance logs: $logs")
                        
                        if (logs.isNotEmpty()) {
                            attendanceAdapter.updateLogs(logs)
                            binding.rvAttendanceLogs.visibility = View.VISIBLE
                            binding.tvNoLogs.visibility = View.GONE
                        } else {
                            binding.rvAttendanceLogs.visibility = View.GONE
                            binding.tvNoLogs.visibility = View.VISIBLE
                        }
                    } else {
                        Log.d("HomePanelFragment", "API call failed: ${response.message()}")
                        Toast.makeText(requireContext(), "Failed to fetch attendance logs", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomePanelFragment", "Error fetching attendance: ${e.message}")
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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

    private suspend fun fetchNotifications() {
        try {
            val token = SessionManager.getToken(requireContext())?.let { "Bearer $it" }
                ?: throw Exception("No authentication token found")

            val response = apiService.getNotifications(token)
            if (response.isSuccessful) {
                val notifications = response.body()?.notifications ?: emptyList()
                val unreadCount = notifications.count { !it.is_read }
                updateNotificationBadge(unreadCount)
            } else {
                Log.e("Notifications", "Failed to fetch notifications: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("Notifications", "Error fetching notifications: ${e.message}")
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
        // Fetch notifications on resume to update badge
        if (SessionManager.getRole(requireContext()) != "professor") {
            lifecycleScope.launch {
                fetchNotifications()
            }
        }
    }

    private fun updateNotificationBadge(count: Int) {
        binding.notificationBadge.apply {
            text = if (count > 99) "99+" else count.toString()
            visibility = if (count > 0) View.VISIBLE else View.GONE
            
            // Animate badge if it becomes visible
            if (count > 0 && visibility == View.VISIBLE) {
                alpha = 0f
                scaleX = 0f
                scaleY = 0f
                animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
