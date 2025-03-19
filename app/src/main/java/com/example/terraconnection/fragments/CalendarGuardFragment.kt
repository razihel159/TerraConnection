package com.example.terraconnection.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.adapters.LinkedStudentAdapter
import com.example.terraconnection.adapters.AttendanceLogAdapter
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.api.StudentStatus
import com.example.terraconnection.databinding.FragmentCalendarGuardBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CalendarGuardFragment : Fragment(R.layout.fragment_calendar_guard) {
    private var _binding: FragmentCalendarGuardBinding? = null
    private val binding get() = _binding!!
    private lateinit var studentAdapter: LinkedStudentAdapter
    private lateinit var attendanceAdapter: AttendanceLogAdapter
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private var selectedStudent: StudentStatus? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarGuardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupCalendar()
        fetchLinkedStudents()
    }

    private fun setupRecyclerViews() {
        // Setup student list
        binding.studentList.layoutManager = LinearLayoutManager(requireContext())
        studentAdapter = LinkedStudentAdapter { student ->
            selectedStudent = student
            fetchStudentAttendance()
        }
        binding.studentList.adapter = studentAdapter

        // Setup attendance logs
        binding.attendanceLogs.layoutManager = LinearLayoutManager(requireContext())
        attendanceAdapter = AttendanceLogAdapter()
        binding.attendanceLogs.adapter = attendanceAdapter
    }

    private fun setupCalendar() {
        binding.guardViewCal.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            selectedStudent?.let {
                fetchStudentAttendance()
            }
        }
    }

    private fun fetchLinkedStudents() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getLinkedStudents("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val students = response.body()?.students ?: emptyList()
                        if (students.isNotEmpty()) {
                            studentAdapter.updateStudents(students)
                            binding.studentList.visibility = View.VISIBLE
                            binding.noStudentsText.visibility = View.GONE
                        } else {
                            binding.studentList.visibility = View.GONE
                            binding.noStudentsText.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch linked students", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CalendarGuardFragment", "Error fetching linked students: ${e.message}")
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchStudentAttendance() {
        val student = selectedStudent ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getChildStatus(
                    "Bearer $token",
                    student.id.toString()
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val status = response.body()
                        if (status?.lastLog != null) {
                            attendanceAdapter.updateLogs(listOf(status.lastLog))
                            binding.attendanceLogs.visibility = View.VISIBLE
                            binding.noLogsText.visibility = View.GONE
                        } else {
                            binding.attendanceLogs.visibility = View.GONE
                            binding.noLogsText.visibility = View.VISIBLE
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch attendance logs", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CalendarGuardFragment", "Error fetching attendance: ${e.message}")
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 