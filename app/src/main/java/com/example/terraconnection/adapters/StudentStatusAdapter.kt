package com.example.terraconnection.adapters

import android.os.Build
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
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.example.terraconnection.utils.LocationFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class StudentStatusAdapter : RecyclerView.Adapter<StudentStatusAdapter.StudentStatusViewHolder>() {
    private var students: List<StudentStatus> = listOf()

    fun updateStudents(newStudents: List<StudentStatus>) {
        students = newStudents
        notifyDataSetChanged()
    }

    inner class StudentStatusViewHolder(private val binding: ItemStudentStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var bindJob: Job? = null
        
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(student: StudentStatus) {
            bindJob?.cancel()
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

                // Default location placeholders
                studentLocationText.text = "Location unavailable"
                studentLocationUpdatedText.isVisible = false

                val lastGps = student.lastGPS
                if (lastGps != null) {
                    // Parse timestamp to epoch millis (expected ISO format)
                    val timeMillis = try {
                        java.time.Instant.parse(lastGps.timestamp).toEpochMilli()
                    } catch (e: Exception) {
                        Log.w("StudentStatusAdapter", "Failed to parse GPS timestamp: ${lastGps.timestamp}")
                        null
                    }

                    val isRecent = timeMillis?.let { LocationFormatter.isRecent(it) } == true
                    studentLocationText.alpha = if (isRecent) 1f else 0.75f

                    // Show loading while resolving area
                    studentLocationText.text = if (isRecent) "Determining area…" else "Resolving last area…"
                    studentLocationUpdatedText.isVisible = timeMillis != null
                    if (timeMillis != null) {
                        studentLocationUpdatedText.text = if (isRecent) {
                            "Updated ${LocationFormatter.formatRelative(timeMillis)}"
                        } else {
                            "Last seen ${LocationFormatter.formatRelative(timeMillis)}"
                        }
                    }

                    val lat = lastGps.latitude
                    val lon = lastGps.longitude
                    val context = root.context.applicationContext

                    bindJob = CoroutineScope(Dispatchers.Main).launch {
                        val area = LocationFormatter.getGeneralAreaName(context, lat, lon)
                        val text = when {
                            area.isNullOrBlank() -> "Location unavailable"
                            isRecent -> "Location: $area"
                            else -> "Last seen near $area"
                        }
                        // Guard recycled holder
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            studentLocationText.text = text
                        }
                    }
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: StudentStatusViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount(): Int = students.size
} 