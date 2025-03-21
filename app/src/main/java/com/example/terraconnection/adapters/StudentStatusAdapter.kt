package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.api.StudentStatus
import com.example.terraconnection.databinding.ItemStudentStatusBinding
import com.example.terraconnection.api.RetrofitClient
import com.bumptech.glide.Glide
import android.util.Log

class StudentStatusAdapter : RecyclerView.Adapter<StudentStatusAdapter.StudentStatusViewHolder>() {
    private var students: List<StudentStatus> = listOf()

    fun updateStudents(newStudents: List<StudentStatus>) {
        students = newStudents
        notifyDataSetChanged()
    }

    inner class StudentStatusViewHolder(private val binding: ItemStudentStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(student: StudentStatus) {
            with(binding) {
                Log.d("StudentStatusAdapter", "Binding student: ${student.first_name} ${student.last_name}")
                Log.d("StudentStatusAdapter", "Student profile picture: ${student.profile_picture}")
                Log.d("StudentStatusAdapter", "User object: ${student.user}")
                Log.d("StudentStatusAdapter", "User profile picture: ${student.user?.profile_picture}")
                
                // Set student name
                studentName.text = "${student.first_name} ${student.last_name}"
                
                // Set status text
                studentStatusText.text = if (student.onCampus) "On Campus" else "Off Campus"
                
                // Set status indicator background
                statusIndicator.setBackgroundResource(
                    if (student.onCampus) R.drawable.status_indicator_green 
                    else R.drawable.status_indicator_red
                )

                // Try to get profile picture from either student or user object
                val profilePicture = when {
                    !student.profile_picture.isNullOrEmpty() -> student.profile_picture
                    !student.user?.profile_picture.isNullOrEmpty() -> student.user?.profile_picture
                    else -> null
                }

                if (!profilePicture.isNullOrEmpty()) {
                    val profilePicUrl = if (profilePicture.startsWith("/")) {
                        RetrofitClient.BASE_URL.removeSuffix("/") + profilePicture
                    } else {
                        RetrofitClient.BASE_URL.removeSuffix("/") + "/" + profilePicture
                    }
                    
                    Log.d("StudentStatusAdapter", "Loading profile picture from URL: $profilePicUrl")
                    
                    Glide.with(root.context)
                        .load(profilePicUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(studentProfilePic)
                } else {
                    Log.d("StudentStatusAdapter", "No profile picture available, loading placeholder")
                    Glide.with(root.context)
                        .load(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(studentProfilePic)
                }
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

    override fun getItemCount(): Int = students.size
} 