package com.example.terraconnection.fragments

import android.content.Intent
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
import com.example.terraconnection.activities.ListStudentActivity
import com.example.terraconnection.adapters.ScheduleAdapter
import com.example.terraconnection.adapters.OnScheduleClickListener
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
    private lateinit var scheduleAdapter: ScheduleAdapter
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

        // ✅ Set up RecyclerView as horizontal
        binding.profSched.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        scheduleAdapter = ScheduleAdapter(mutableListOf(), object : OnScheduleClickListener {
            override fun onScheduleClick(schedule: Schedule) {
                navigateToListStudentActivity(schedule)
            }
        })
        binding.profSched.adapter = scheduleAdapter

        // ✅ Fetch both professor and student schedules
        fetchSchedules()

        // ✅ Calendar date change listener
        binding.schedViewCal.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            filterSchedules(selectedDate)
        }
    }

    private fun fetchSchedules() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(requireContext())
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Fetch professor schedule
                val profResponse = RetrofitClient.apiService.getProfessorSchedule("Bearer $token")
                val studentResponse = RetrofitClient.apiService.getStudentSchedule("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (profResponse.isSuccessful && studentResponse.isSuccessful) {
                        val profSchedule = profResponse.body()?.schedule ?: emptyList()
                        val studentSchedule = studentResponse.body()?.schedule ?: emptyList()

                        scheduleList = profSchedule + studentSchedule

                        // ✅ Log schedule list to check if it's empty
                        Log.d("CalendarProfFragment", "Fetched schedules: $scheduleList")

                        if (scheduleList.isNotEmpty()) {
                            scheduleAdapter.updateList(scheduleList.toMutableList())
                            binding.profSched.visibility = View.VISIBLE
                        } else {
                            Log.d("CalendarProfFragment", "No schedules received from API.")
                            binding.profSched.visibility = View.GONE
                            Toast.makeText(requireContext(), "No schedule available", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("CalendarProfFragment", "API call failed: ${profResponse.message()} / ${studentResponse.message()}")
                        Toast.makeText(requireContext(), "Failed to fetch schedules", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CalendarProfFragment", "Error fetching schedules: ${e.message}")
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterSchedules(selectedDate: String) {
        Log.d("CalendarProfFragment", "Filtering for date: $selectedDate")

        val filteredSchedules = scheduleList.filter { schedule ->
            val matches = schedule.schedule.split(",").any { day ->
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
                Log.d("CalendarProfFragment", "Checking schedule: ${schedule.schedule}")
                day.trim() == selectedDay
            }
            matches
        }

        Log.d("CalendarProfFragment", "Filtered schedules: $filteredSchedules")

        if (filteredSchedules.isNotEmpty()) {
            scheduleAdapter.updateList(filteredSchedules.toMutableList())
            binding.profSched.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Schedule loaded for $selectedDate", Toast.LENGTH_SHORT).show() // ✅ Schedule loaded toast
        } else {
            scheduleAdapter.updateList(mutableListOf())
            binding.profSched.visibility = View.GONE
            Toast.makeText(requireContext(), "No schedule available for $selectedDate", Toast.LENGTH_SHORT).show() // ✅ No schedule toast
            Log.d("CalendarProfFragment", "No matching schedules for selected date: $selectedDate")
        }
    }

    private fun navigateToListStudentActivity(schedule: Schedule) {
        val intent = Intent(requireContext(), ListStudentActivity::class.java).apply {
            putExtra("classCode", schedule.class_code)
            putExtra("className", schedule.class_name)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
