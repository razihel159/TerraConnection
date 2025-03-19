package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.api.StudentStatus
import com.example.terraconnection.databinding.ItemStudentStatusBinding

class StudentStatusAdapter : RecyclerView.Adapter<StudentStatusAdapter.StudentStatusViewHolder>() {
    private var students: List<StudentStatus> = emptyList()

    inner class StudentStatusViewHolder(private val binding: ItemStudentStatusBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(student: StudentStatus) {
            binding.apply {
                studentName.text = "${student.first_name} ${student.last_name}"
                
                // Update status indicator and text
                statusIndicator.setBackgroundResource(
                    if (student.onCampus) R.drawable.status_indicator_green 
                    else R.drawable.status_indicator_red
                )
                studentStatusText.text = if (student.onCampus) "On Campus" else "Off Campus"
                studentStatusText.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        if (student.onCampus) R.color.green else R.color.red
                    )
                )

                // Set default profile picture
                studentProfilePic.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentStatusViewHolder {
        val binding = ItemStudentStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentStatusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentStatusViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount() = students.size

    fun updateStudents(newStudents: List<StudentStatus>) {
        students = newStudents
        notifyDataSetChanged()
    }
} 