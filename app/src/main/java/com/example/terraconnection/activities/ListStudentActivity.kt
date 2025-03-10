package com.example.terraconnection.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terraconnection.adapters.StudentAdapter
import com.example.terraconnection.data.Student
import com.example.terraconnection.databinding.ActivityListStudentBinding
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.data.NotificationRequest
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListStudentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListStudentBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var studentAdapter: StudentAdapter
    private var studentList: List<Student> = emptyList()
    private val apiService = RetrofitClient.apiService
    private var currentClassId: String = "-1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("TerraPrefs", Context.MODE_PRIVATE)

        // Initialize RecyclerView
        setupRecyclerView()
        setupNotificationButton()

        // Get class details from intent
        val classId = intent.getIntExtra("class_id", -1)
        currentClassId = classId.toString()
        val classCode = intent.getStringExtra("class_code") ?: ""
        val className = intent.getStringExtra("class_name") ?: ""
        val room = intent.getStringExtra("room") ?: ""
        val time = intent.getStringExtra("time") ?: ""

        // Update class info in UI
        binding.className.text = "$className ($classCode)"
        binding.roomText.text = "Room: $room"
        binding.timeText.text = "Time: $time"

        if (classId == -1) {
            Toast.makeText(this, "Invalid class ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Get today's date in YYYY-MM-DD format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        // Fetch attendance data
        fetchAttendanceData(classId.toString(), today)
    }

    private fun setupRecyclerView() {
        binding.studentList.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(studentList)
        binding.studentList.adapter = studentAdapter
    }

    private fun fetchAttendanceData(classId: String, date: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(this@ListStudentActivity)?.let { "Bearer $it" }
                    ?: throw Exception("No authentication token found")

                Log.d("ListStudentActivity", "Fetching attendance for classId: $classId, date: $date")
                val response = apiService.getProfessorAttendance(token, date, classId)
                
                withContext(Dispatchers.Main) {
                    binding.loadingIndicator.visibility = View.GONE
                    
                    if (response.isSuccessful) {
                        val attendanceData = response.body()
                        if (attendanceData != null) {
                            Log.d("ListStudentActivity", "Attendance data: $attendanceData")
                            
                            // First, fetch student names from class enrollment
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val enrollmentResponse = apiService.getClassEnrollment(token, classId)
                                    if (enrollmentResponse.isSuccessful) {
                                        val enrollmentData = enrollmentResponse.body()
                                        val studentMap = enrollmentData?.enrollments?.associate { enrollment ->
                                            enrollment.studentId to "${enrollment.student.firstName} ${enrollment.student.lastName}"
                                        } ?: emptyMap()

                                        withContext(Dispatchers.Main) {
                                            studentList = attendanceData.attendance.map { studentAttendance ->
                                                // Check if student has any logs for today
                                                val hasLogs = studentAttendance.logs.isNotEmpty()
                                                // If they have logs, check their last scan type
                                                val isOnCampus = if (hasLogs) {
                                                    // Get the most recent log
                                                    val lastLog = studentAttendance.logs.maxByOrNull { it.timestamp }
                                                    // If the last scan was "entry", they're on campus
                                                    lastLog?.type == "entry"
                                                } else {
                                                    false
                                                }
                                                val studentName = studentMap[studentAttendance.studentId] ?: "Unknown Student"
                                                
                                                Student(
                                                    id = studentAttendance.studentId,
                                                    name = studentName,
                                                    onCampus = isOnCampus,
                                                    role = "student",
                                                    statusIndicator = if (isOnCampus) R.drawable.ic_present else R.drawable.ic_absent,
                                                    notifyIcon = R.drawable.ic_notify
                                                )
                                            }
                                            studentAdapter.updateStudents(studentList)
                                            
                                            if (studentList.isEmpty()) {
                                                Toast.makeText(
                                                    this@ListStudentActivity,
                                                    "No students found for this class",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                this@ListStudentActivity,
                                                "Failed to fetch student information",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ListStudentActivity", "Error fetching enrollment data: ${e.message}")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@ListStudentActivity,
                                            "Error: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@ListStudentActivity,
                                "No attendance data available",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@ListStudentActivity,
                            "Failed to fetch attendance data: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ListStudentActivity", "Error fetching attendance: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.loadingIndicator.visibility = View.GONE
                    Toast.makeText(
                        this@ListStudentActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupNotificationButton() {
        binding.notifyAllButton.setOnClickListener {
            val className = intent.getStringExtra("class_name") ?: ""
            val room = intent.getStringExtra("room") ?: ""
            sendNotificationToClass(className, room)
        }
    }

    private fun updateNotificationButtonState(enabled: Boolean) {
        binding.notifyAllButton.apply {
            isEnabled = enabled
            alpha = if (enabled) 1.0f else 0.5f
        }
    }

    private fun sendNotificationToClass(className: String, room: String) {
        updateNotificationButtonState(false)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(this@ListStudentActivity)?.let { "Bearer $it" }
                    ?: throw Exception("No authentication token found")

                val notificationRequest = NotificationRequest(
                    classId = currentClassId,
                    title = "Class Reminder",
                    message = "Please proceed to $className class in Room $room"
                )

                val response = apiService.sendNotification(token, notificationRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@ListStudentActivity,
                            "Notification sent to all students",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Start cooldown timer
                        startCooldownTimer(300) // 5 minutes default
                    } else {
                        // Check if it's a cooldown error
                        if (response.code() == 429) {
                            val errorBody = response.errorBody()?.string()
                            if (errorBody?.contains("remainingSeconds") == true) {
                                val remainingSeconds = errorBody.substringAfter("remainingSeconds\":").substringBefore("}")
                                    .trim().toIntOrNull() ?: 300
                                startCooldownTimer(remainingSeconds)
                                val minutes = remainingSeconds / 60
                                val seconds = remainingSeconds % 60
                                Toast.makeText(
                                    this@ListStudentActivity,
                                    "Please wait $minutes minutes and $seconds seconds before sending another notification",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                updateNotificationButtonState(true)
                                Toast.makeText(
                                    this@ListStudentActivity,
                                    "Please wait before sending another notification",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            updateNotificationButtonState(true)
                            Toast.makeText(
                                this@ListStudentActivity,
                                "Failed to send notification",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ListStudentActivity", "Error sending notification: ${e.message}")
                withContext(Dispatchers.Main) {
                    updateNotificationButtonState(true)
                    Toast.makeText(
                        this@ListStudentActivity,
                        "Error sending notification: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startCooldownTimer(seconds: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            var remainingSeconds = seconds
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                if (remainingSeconds == 0) {
                    updateNotificationButtonState(true)
                }
            }
        }
    }
}
