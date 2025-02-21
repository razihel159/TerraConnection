package com.example.terraconnection.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.activities.ListStudentActivity
import com.example.terraconnection.adapters.ProfSchedAdapter
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.databinding.FragmentCalendarProfBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CalendarProfFragment : Fragment(R.layout.fragment_calendar_prof) {
    private var _binding: FragmentCalendarProfBinding? = null
    private val binding get() = _binding!!
    private lateinit var scheduleAdapter: ProfSchedAdapter
    private var scheduleList: List<Schedule> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarProfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Set up RecyclerView with click listener
        binding.profSched.layoutManager = LinearLayoutManager(requireContext())
        scheduleAdapter = ProfSchedAdapter(emptyList()) { selectedSchedule ->
            navigateToListStudentActivity(selectedSchedule)
        }
        binding.profSched.adapter = scheduleAdapter

        // ✅ Fetch schedule data
        fetchSchedule()

        // ✅ Calendar date change listener
        binding.schedViewCal.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            filterSchedule(selectedDate)
        }
    }

    private fun fetchSchedule() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getProfessorSchedule("Bearer $token")
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val scheduleResponse = response.body()
                        if (scheduleResponse != null && scheduleResponse.schedule.isNotEmpty()) {
                            scheduleList = scheduleResponse.schedule
                            scheduleAdapter.updateList(scheduleList) // ✅ Update adapter
                            Toast.makeText(requireContext(), "Schedule Loaded!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "No schedule available", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch schedule", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterSchedule(selectedDate: String) {
        val filteredSchedules = scheduleList.filter { schedule ->
            schedule.scheduleDay.split(",").any { day ->
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(selectedDate)
                calendar.time = date
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val selectedDay = when (dayOfWeek) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
                day.trim() == selectedDay
            }
        }

        if (filteredSchedules.isNotEmpty()) {
            scheduleAdapter.updateList(filteredSchedules)
            binding.profSched.visibility = View.VISIBLE
        } else {
            binding.profSched.visibility = View.GONE
            Toast.makeText(requireContext(), "No schedule for $selectedDate", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToListStudentActivity(schedule: Schedule) {
        val intent = Intent(requireContext(), ListStudentActivity::class.java).apply {
            putExtra("classCode", schedule.classCode)
            putExtra("className", schedule.className)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
