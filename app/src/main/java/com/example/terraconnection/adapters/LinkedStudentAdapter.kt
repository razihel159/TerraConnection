package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.databinding.ItemLinkedStudentBinding
import com.example.terraconnection.api.StudentStatus

class LinkedStudentAdapter(
    private var students: MutableList<StudentStatus> = mutableListOf(),
    private val onStudentClick: (StudentStatus) -> Unit
) : RecyclerView.Adapter<LinkedStudentAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemLinkedStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(student: StudentStatus) {
            binding.apply {
                studentName.text = "${student.first_name} ${student.last_name}"
                studentId.text = student.school_id
                
                root.setOnClickListener {
                    onStudentClick(student)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLinkedStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount(): Int = students.size

    fun updateStudents(newStudents: List<StudentStatus>) {
        students.clear()
        students.addAll(newStudents)
        notifyDataSetChanged()
    }
} 