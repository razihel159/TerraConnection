package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.data.Student
import com.example.terraconnection.databinding.StudentItemBinding

class StudentAdapter(
    private var studentList: List<Student>,
    private val onNotifyClick: (String) -> Unit // Callback for notification button click
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = StudentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]

        // Bind student name
        holder.binding.studentName.text = student.name

        // Set status indicator color properly
        val statusColor = if (student.onCampus) R.color.green else R.color.red
        holder.binding.statusIndicator.setBackgroundColor(
            ContextCompat.getColor(holder.binding.root.context, statusColor)
        )

        // Handle notification button click
        holder.binding.notifyButton.setOnClickListener {
            onNotifyClick(student.name)
        }
    }

    override fun getItemCount(): Int = studentList.size

    fun updateList(newStudentList: List<Student>) {
        studentList = newStudentList
        notifyDataSetChanged()
    }

    class StudentViewHolder(val binding: StudentItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}
