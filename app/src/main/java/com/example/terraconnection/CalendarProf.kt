package com.example.terraconnection

import ProfSchedAdapter
import android.os.Bundle
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.data.ClassSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CalendarProf : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var scheduleAdapter: ProfSchedAdapter
    private lateinit var calendarView: CalendarView
    private var scheduleList: List<ClassSchedule> = emptyList() // ✅ Stores fetched schedules

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_prof)

        recyclerView = findViewById(R.id.profSched)
        calendarView = findViewById(R.id.schedViewCal)

        recyclerView.layoutManager = LinearLayoutManager(this)
        scheduleAdapter = ProfSchedAdapter(emptyList())
        recyclerView.adapter = scheduleAdapter

        fetchSchedule() // ✅ Fetch schedule data when activity starts

        // ✅ Calendar Date Selection Listener
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            filterSchedule(selectedDate) // ✅ Filter schedules based on selected date
        }
    }

    private fun fetchSchedule() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = SessionManager.getToken(this@CalendarProf)

                if (token.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CalendarProf, "No token found, please log in again", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getStudentSchedule("Bearer $token")

                if (response.isSuccessful) {
                    val schedules = response.body()
                    if (!schedules.isNullOrEmpty()) {
                        scheduleList = schedules // ✅ Store schedules in global list
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CalendarProf, "Schedule Loaded!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CalendarProf, "No schedule available", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
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
        val filteredSchedules = scheduleList.filter { it.date == selectedDate }

        if (filteredSchedules.isNotEmpty()) {
            scheduleAdapter.updateList(filteredSchedules)
            recyclerView.visibility = RecyclerView.VISIBLE
        } else {
            recyclerView.visibility = RecyclerView.GONE
            Toast.makeText(this, "No schedule for $selectedDate", Toast.LENGTH_SHORT).show()
        }
    }
}
