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
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    
    private val dateDisplayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarStudBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCalendar()
        
        // Set initial date display
        updateDateDisplay(Date())
        
        // Fetch initial attendance for today's date
        fetchAttendance(selectedDate)
    }

    private fun setupRecyclerView() {
        binding.attendanceLogs.layoutManager = LinearLayoutManager(requireContext())
        attendanceAdapter = AttendanceLogAdapter()
        binding.attendanceLogs.adapter = attendanceAdapter
    }

    private fun setupCalendar() {
        // Set today's date as selected
        binding.studViewCal.date = System.currentTimeMillis()

        // Calendar date change listener
        binding.studViewCal.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            val date = calendar.time
            
            // Update the selected date display
            updateDateDisplay(date)
            
            // Update the API date format for fetching
            selectedDate = apiDateFormat.format(date)
            
            // Show loading state
            showLoadingState()
            
            // Fetch attendance for the selected date
            fetchAttendance(selectedDate)
        }
    }

    private fun updateDateDisplay(date: Date) {
        binding.selectedDateDisplay.text = dateDisplayFormat.format(date)
    }

    private fun showLoadingState() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.attendanceLogs.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.loadingIndicator.visibility = View.GONE
        binding.attendanceLogs.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
    }

    private fun showAttendanceLogs() {
        binding.loadingIndicator.visibility = View.GONE
        binding.attendanceLogs.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE
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
                            showAttendanceLogs()
                        } else {
                            showEmptyState()
                        }
                    } else {
                        Log.d("CalendarStudFragment", "API call failed: ${response.message()}")
                        Toast.makeText(requireContext(), "Failed to fetch attendance logs", Toast.LENGTH_SHORT).show()
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CalendarStudFragment", "Error fetching attendance: ${e.message}")
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 