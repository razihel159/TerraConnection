package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.data.Schedule

class ProfSchedAdapter(private var schedules: List<Schedule>) : 
    RecyclerView.Adapter<ProfSchedAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val classCode: TextView = itemView.findViewById(R.id.classCode)
        val className: TextView = itemView.findViewById(R.id.className)
        val room: TextView = itemView.findViewById(R.id.room)
        val time: TextView = itemView.findViewById(R.id.time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.classCode.text = schedule.classCode
        holder.className.text = schedule.className
        holder.room.text = schedule.room
        holder.time.text = "${schedule.startTime} - ${schedule.endTime}"
    }

    override fun getItemCount() = schedules.size

    fun updateList(newList: List<Schedule>) {
        schedules = newList
        notifyDataSetChanged()
    }
}
