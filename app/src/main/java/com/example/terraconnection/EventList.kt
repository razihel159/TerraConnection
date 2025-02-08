package com.example.terraconnection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.databinding.ActivityEventListBinding

class EventListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEventListBinding
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerView: RecyclerView = binding.calendarRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 🔹 Sample event data (Date & Time only)
        val eventList = listOf(
            Event("", ""),
            Event("", ""),
            Event("", ""),
            Event("", ""),
            Event("", "")
        )

        eventAdapter = EventAdapter(eventList)
        recyclerView.adapter = eventAdapter
    }
}
