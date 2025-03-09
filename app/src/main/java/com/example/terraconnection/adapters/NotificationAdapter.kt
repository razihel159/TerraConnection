package com.example.terraconnection.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.data.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.titleText)
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val classText: TextView = itemView.findViewById(R.id.classText)
        val senderText: TextView = itemView.findViewById(R.id.senderText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notif, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        holder.titleText.text = notification.title
        holder.messageText.text = notification.message
        holder.classText.text = "${notification.class_code} - ${notification.class_name}"
        holder.senderText.text = "From: ${notification.sender_name}"

        // Format the time as relative time span
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = dateFormat.parse(notification.created_at)
        val now = System.currentTimeMillis()
        
        date?.let {
            holder.timeText.text = DateUtils.getRelativeTimeSpanString(
                it.time,
                now,
                DateUtils.MINUTE_IN_MILLIS
            )
        }
    }

    override fun getItemCount(): Int = notifications.size
}
