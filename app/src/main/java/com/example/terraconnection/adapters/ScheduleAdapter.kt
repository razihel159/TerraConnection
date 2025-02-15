package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.databinding.ItemScheduleBinding
import com.example.terraconnection.data.Schedule
import java.text.SimpleDateFormat
import java.util.Locale

class ScheduleAdapter(
    private val schedules: List<Schedule>,
    private val listener: OnScheduleClickListener
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    private val timeInputFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val timeOutputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private fun formatTime(time: String): String {
        return try {
            val date = timeInputFormat.parse(time)
            date?.let { timeOutputFormat.format(it) } ?: time
        } catch (e: Exception) {
            time
        }
    }

    inner class ViewHolder(private val binding: ItemScheduleBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(schedule: Schedule) {
            binding.apply {
                classCode.text = schedule.class_code
                className.text = schedule.class_name
                room.text = schedule.room
                
                // Format start and end times
                val formattedStartTime = formatTime(schedule.start_time)
                val formattedEndTime = formatTime(schedule.end_time)
                time.text = "$formattedStartTime - $formattedEndTime"
                
                scheduleDay.text = schedule.schedule

                root.setOnClickListener {
                    listener.onScheduleClick(schedule)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(schedules[position])
    }

    override fun getItemCount() = schedules.size
}
