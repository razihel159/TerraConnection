package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import android.widget.ImageView
import com.example.terraconnection.R
import com.example.terraconnection.data.Student

class StudentAdapter(
    private var students: List<Student>,
    private val onNotifyClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentName: TextView = itemView.findViewById(R.id.studentName)
        val statusIndicator: ImageView = itemView.findViewById(R.id.statusIndicator)
        val notifyButton: ImageButton = itemView.findViewById(R.id.notifyButton)
        val cardView: CardView = itemView.findViewById(R.id.main)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.student_item, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        holder.studentName.text = student.name
        holder.statusIndicator.setImageResource(student.statusIndicator)
        holder.notifyButton.setImageResource(student.notifyIcon)

        holder.notifyButton.setOnClickListener {
            onNotifyClick(student)
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateStudents(newStudents: List<Student>) {
        students = newStudents
        notifyDataSetChanged()
    }
}
