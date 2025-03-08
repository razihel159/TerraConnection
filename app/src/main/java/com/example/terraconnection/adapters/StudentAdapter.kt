package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.data.Student
import com.example.terraconnection.databinding.StudentItemBinding

class StudentAdapter(
    private var students: List<Student>
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(private val binding: StudentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(student: Student) {
            binding.studentName.text = student.name
            binding.statusIcon.setImageResource(student.statusIndicator)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = StudentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount() = students.size

    fun updateStudents(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }
}
