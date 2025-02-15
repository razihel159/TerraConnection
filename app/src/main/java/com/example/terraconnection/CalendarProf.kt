package com.example.terraconnection

import android.os.Bundle
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.adapters.ProfSchedAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CalendarProf : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var scheduleAdapter: ProfSchedAdapter
    private lateinit var calendarView: CalendarView
    private var scheduleList: List<Schedule> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_prof)

        recyclerView = findViewById(R.id.profSched)
        calendarView = findViewById(R.id.schedViewCal)

        recyclerView.layoutManager = LinearLayoutManager(this)
        scheduleAdapter = ProfSchedAdapter(emptyList())
        recyclerView.adapter = scheduleAdapter

        fetchSchedule()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            filterSchedule(selectedDate)
        }
    }

    private fun fetchSchedule() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(this@CalendarProf)

                if (token == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CalendarProf, "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getProfessorSchedule("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val scheduleResponse = response.body()
                        if (scheduleResponse != null && scheduleResponse.schedule.isNotEmpty()) {
                            scheduleList = scheduleResponse.schedule
                            Toast.makeText(this@CalendarProf, "Schedule Loaded!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CalendarProf, "No schedule available", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@CalendarProf, "Failed to fetch schedule", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CalendarProf, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            recyclerView.visibility = RecyclerView.VISIBLE
        } else {
            recyclerView.visibility = RecyclerView.GONE
            Toast.makeText(this, "No schedule for $selectedDate", Toast.LENGTH_SHORT).show()
        }
    }
}
