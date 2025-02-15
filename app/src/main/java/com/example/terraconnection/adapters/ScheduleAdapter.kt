package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.data.Schedule

class ScheduleAdapter(
    private val scheduleList: List<Schedule>,
    private val listener: OnScheduleClickListener
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubjectCode: TextView = view.findViewById(R.id.tvSubjectCode)
        val tvSubjectName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvRoom: TextView = view.findViewById(R.id.tvRoom)
        val tvTime: TextView = view.findViewById(R.id.tvTime)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onScheduleClick(scheduleList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = scheduleList[position]
        holder.tvSubjectCode.text = schedule.subjectCode
        holder.tvSubjectName.text = schedule.subjectName
        holder.tvRoom.text = "Room: ${schedule.room}"
        holder.tvTime.text = schedule.time
    }

    override fun getItemCount(): Int = scheduleList.size
}
