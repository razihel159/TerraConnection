package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.data.AttendanceLog
import com.example.terraconnection.databinding.ItemAttendanceLogBinding
import java.text.SimpleDateFormat
import java.util.*

class AttendanceLogAdapter : RecyclerView.Adapter<AttendanceLogAdapter.LogViewHolder>() {
    private var logs: List<AttendanceLog> = emptyList()

    fun updateLogs(newLogs: List<AttendanceLog>) {
        logs = newLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemAttendanceLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    class LogViewHolder(private val binding: ItemAttendanceLogBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(log: AttendanceLog) {
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val time = dateFormat.format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(log.timestamp))
            
            binding.logType.text = log.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            binding.logTime.text = time

            // Set icon based on log type
            binding.logIcon.setImageResource(
                when (log.type.lowercase()) {
                    "entry" -> android.R.drawable.ic_menu_send
                    "exit" -> android.R.drawable.ic_menu_close_clear_cancel
                    else -> android.R.drawable.ic_menu_help
                }
            )
        }
    }
} 