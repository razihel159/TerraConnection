package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.data.AttendanceLog
import com.example.terraconnection.databinding.ItemAttendanceLogBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
            binding.logType.text = resolveDisplayType(log.type)
            binding.logTime.text = resolveDisplayTime(log.timestamp)

            // Set icon based on log type
            binding.logIcon.setImageResource(
                when (normaliseType(log.type)) {
                    EventType.ENTRY -> android.R.drawable.ic_menu_send
                    EventType.EXIT -> android.R.drawable.ic_menu_close_clear_cancel
                    EventType.UNKNOWN -> android.R.drawable.ic_menu_help
                }
            )
        }

        private fun resolveDisplayType(rawType: String): String {
            return when (normaliseType(rawType)) {
                EventType.ENTRY -> "Entry"
                EventType.EXIT -> "Exit"
                EventType.UNKNOWN -> {
                    val cleaned = rawType.trim()
                    if (cleaned.isEmpty()) {
                        "Unknown"
                    } else {
                        cleaned.replaceFirstChar { ch ->
                            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                        }.replace('_', ' ')
                    }
                }
            }
        }

        private fun resolveDisplayTime(timestamp: String?): String {
            if (timestamp.isNullOrBlank()) return "N/A"

            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            // Accept multiple timestamp formats because different services return slightly different ISO variants.
            val parsePatterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            )

            for (pattern in parsePatterns) {
                try {
                    val parser = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val parsedDate = parser.parse(timestamp)
                    if (parsedDate != null) {
                        return outputFormat.format(parsedDate)
                    }
                } catch (_: Exception) {
                    // Try the next pattern if parsing fails
                }
            }

            return timestamp
        }

        private fun normaliseType(rawType: String?): EventType {
            if (rawType.isNullOrBlank()) return EventType.UNKNOWN
            val normalised = rawType.trim().lowercase(Locale.ROOT)
            return when {
                normalised.contains("entry") -> EventType.ENTRY
                // Treat any exit-related descriptor (including legacy logout) the same so visuals stay consistent.
                normalised.contains("exit") || normalised.contains("logout") -> EventType.EXIT
                else -> EventType.UNKNOWN
            }
        }

        private enum class EventType {
            ENTRY, EXIT, UNKNOWN
        }
    }
}
