package com.example.terraconnection.activities

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.databinding.StudentItemBinding

class StudentAdapter(private val onNotifyClick: (String) -> Unit) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private val studentList = listOf("John Doe", "Jane Smith", "Alice Brown") // Sample data

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = StudentItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val studentName = studentList[position]
        holder.bind(studentName)
    }

    override fun getItemCount() = studentList.size

    inner class StudentViewHolder(private val binding: StudentItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(studentName: String) {
            binding.studentName.text = studentName
            binding.notifyButton.setOnClickListener { onNotifyClick(studentName) }
        }
    }
}
