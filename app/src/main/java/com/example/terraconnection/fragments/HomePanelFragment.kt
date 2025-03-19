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
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import com.example.terraconnection.adapters.StudentStatusAdapter

class HomePanelFragment : Fragment(R.layout.fragment_home_panel), OnScheduleClickListener {

    private var _binding: FragmentHomePanelBinding? = null
    private val binding get() = _binding!!
    private val apiService = RetrofitClient.apiService
    private lateinit var attendanceAdapter: AttendanceLogAdapter
    private lateinit var studentStatusAdapter: StudentStatusAdapter

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

        // For guardian role, only show student status
        if (role == "guardian") {
            binding.apply {
                // Hide unnecessary sections
                subjectNotification.visibility = View.GONE
                tvTodaySchedule.visibility = View.GONE
                subjectCard.visibility = View.GONE
                tvNoSchedule.visibility = View.GONE
                attendanceLog.visibility = View.GONE
                
                // Show student status section and setup RecyclerView
                studentStatusContainer.visibility = View.VISIBLE
                setupStudentStatusRecyclerView()
                
                // Start fetching student info
                lifecycleScope.launch {
                    fetchUserDetails()
                    fetchLinkedStudents()
                }
            }
            return
        }

        // Pre-set initial visibility to INVISIBLE instead of GONE to maintain layout
        binding.studentStatusContainer.alpha = 0f
        binding.attendanceLog.alpha = 0f

        when (role) {
            "student" -> {
                binding.studentStatusContainer.visibility = View.GONE
            }
            "guardian" -> {
                binding.subjectNotification.visibility = View.GONE
            }
            "professor" -> {
                binding.attendanceLog.visibility = View.GONE
                binding.studentStatusContainer.visibility = View.GONE
                binding.subjectNotification.visibility = View.GONE
            }
        }

        // Animate visible elements
        if (role != "student") {
            animateViewIn(binding.studentStatusContainer)
        }
        if (role != "professor") {
            animateViewIn(binding.attendanceLog)
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
                        binding.rvAttendanceLogs.visibility = View.GONE
                        binding.tvNoLogs.apply {
                            text = "Please log in again to view logs"
                            visibility = View.VISIBLE
                        }
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
                        binding.rvAttendanceLogs.visibility = View.GONE
                        binding.tvNoLogs.apply {
                            text = "Failed to fetch attendance logs"
                            visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomePanelFragment", "Error fetching attendance: ${e.message}")
                    binding.rvAttendanceLogs.visibility = View.GONE
                    binding.tvNoLogs.apply {
                        text = "Error: ${e.message}"
                        visibility = View.VISIBLE
                    }
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
                    binding.loginGreeting.text = "Hello, $fullName"
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

            // For guardian role, fetch linked student's information
            if (role == "guardian") {
                fetchLinkedStudents()
                return
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
                    binding.subjectCard.visibility = View.VISIBLE
                    binding.tvNoSchedule.visibility = View.GONE
                } else {
                    binding.subjectCard.visibility = View.GONE
                    binding.tvNoSchedule.visibility = View.VISIBLE
                }
            } else {
                binding.subjectCard.visibility = View.GONE
                binding.tvNoSchedule.apply {
                    text = "Failed to load schedule"
                    visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            binding.subjectCard.visibility = View.GONE
            binding.tvNoSchedule.apply {
                text = "Error: ${e.message}"
                visibility = View.VISIBLE
            }
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

    private fun setupStudentStatusRecyclerView() {
        studentStatusAdapter = StudentStatusAdapter()
        binding.rvLinkedStudents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = studentStatusAdapter
        }
    }

    private suspend fun fetchLinkedStudents() {
        try {
            val token = SessionManager.getToken(requireContext())?.let { "Bearer $it" }
                ?: throw Exception("No authentication token found")

            val response = apiService.getLinkedStudents(token)
            if (response.isSuccessful) {
                val linkedStudents = response.body()?.students
                withContext(Dispatchers.Main) {
                    if (linkedStudents.isNullOrEmpty()) {
                        binding.studentStatusContainer.visibility = View.GONE
                        Toast.makeText(
                            requireContext(),
                            "No linked students found",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        studentStatusAdapter.updateStudents(linkedStudents)
                        binding.studentStatusContainer.visibility = View.VISIBLE
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.studentStatusContainer.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch linked students: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.studentStatusContainer.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } finally {
            withContext(Dispatchers.Main) {
                binding.loadingIndicator.visibility = View.GONE
            }
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
