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
import com.example.terraconnection.adapters.AttendanceLogAdapter
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.AttendanceLog
import com.example.terraconnection.data.StudentAttendanceResponse
import com.example.terraconnection.databinding.FragmentCalendarStudBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CalendarStudFragment : Fragment(R.layout.fragment_calendar_stud) {
    private var _binding: FragmentCalendarStudBinding? = null
    private val binding get() = _binding!!
    private lateinit var attendanceAdapter: AttendanceLogAdapter
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // Today's date by default

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarStudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView
        binding.attendanceLogs.layoutManager = LinearLayoutManager(requireContext())
        attendanceAdapter = AttendanceLogAdapter()
        binding.attendanceLogs.adapter = attendanceAdapter

        // Fetch initial attendance for today's date
        fetchAttendance(selectedDate)

        // Calendar date change listener
        binding.studViewCal.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            fetchAttendance(selectedDate)
        }
    }

    private fun fetchAttendance(date: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getStudentAttendance("Bearer $token", date)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val logs = response.body()?.attendance ?: emptyList()
                        Log.d("CalendarStudFragment", "Fetched attendance logs: $logs")
                        
                        if (logs.isNotEmpty()) {
                            attendanceAdapter.updateLogs(logs)
                            binding.attendanceLogs.visibility = View.VISIBLE
                            binding.noLogsText.visibility = View.GONE
                        } else {
                            binding.attendanceLogs.visibility = View.GONE
                            binding.noLogsText.visibility = View.VISIBLE
                        }
                    } else {
                        Log.d("CalendarStudFragment", "API call failed: ${response.message()}")
                        Toast.makeText(requireContext(), "Failed to fetch attendance logs", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CalendarStudFragment", "Error fetching attendance: ${e.message}")
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