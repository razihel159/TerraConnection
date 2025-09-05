package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.terraconnection.R
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.utils.LocationFormatter
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class StudentLocationInfo(
    val studentId: String,
    val studentName: String,
    val profilePicture: String?,
    val role: String = "student",
    val generalArea: String?,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: String?
)

class StudentLocationAdapter : ListAdapter<StudentLocationInfo, StudentLocationAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profileImage)
        val studentName: TextView = itemView.findViewById(R.id.studentName)
        val roleIndicator: TextView = itemView.findViewById(R.id.roleIndicator)
        val locationText: TextView = itemView.findViewById(R.id.locationText)
        val lastUpdatedText: TextView = itemView.findViewById(R.id.lastUpdatedText)
        
        private var bindJob: Job? = null

        fun bind(student: StudentLocationInfo) {
            // Cancel any previous job to avoid race conditions
            bindJob?.cancel()

            studentName.text = student.studentName

            // Set role indicator
            roleIndicator.text = when (student.role) {
                "professor" -> "PROF"
                "student" -> "STUD"
                else -> "USER"
            }
            roleIndicator.setBackgroundResource(
                if (student.role == "professor") R.drawable.rounded_background_professor
                else R.drawable.rounded_background
            )

            // Load profile picture
            if (!student.profilePicture.isNullOrEmpty()) {
                val profilePicUrl = if (student.profilePicture.startsWith("/")) {
                    RetrofitClient.BASE_URL.removeSuffix("/") + student.profilePicture
                } else {
                    RetrofitClient.BASE_URL.removeSuffix("/") + "/" + student.profilePicture
                }
                Glide.with(itemView.context)
                    .load(profilePicUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .circleCrop()
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.default_avatar)
            }

            // Handle location display
            if (student.timestamp != null) {
                try {
                    val timestampMillis = java.time.Instant.parse(student.timestamp).toEpochMilli()
                    val isRecent = LocationFormatter.isRecent(timestampMillis)
                    
                    locationText.alpha = if (isRecent) 1f else 0.75f
                    lastUpdatedText.isVisible = true
                    lastUpdatedText.text = if (isRecent) {
                        "Updated ${LocationFormatter.formatRelative(timestampMillis)}"
                    } else {
                        "Last seen ${LocationFormatter.formatRelative(timestampMillis)}"
                    }

                    // Check if we have general area from backend or need to resolve from coordinates
                    if (student.generalArea != null && student.generalArea.isNotBlank()) {
                        // Use general area from backend
                        val text = if (isRecent) {
                            "Location: ${student.generalArea}"
                        } else {
                            "Last seen near ${student.generalArea}"
                        }
                        locationText.text = text
                    } else if (student.latitude != null && student.longitude != null) {
                        // Fallback: resolve from coordinates (for backward compatibility)
                        locationText.text = if (isRecent) "Determining area…" else "Resolving last area…"
                        
                        bindJob = CoroutineScope(Dispatchers.Main).launch {
                            val area = LocationFormatter.getGeneralAreaName(
                                itemView.context.applicationContext,
                                student.latitude,
                                student.longitude
                            )
                            
                            val text = when {
                                area.isNullOrBlank() -> "Location unavailable"
                                isRecent -> "Location: $area"
                                else -> "Last seen near $area"
                            }
                            
                            // Guard against recycled viewholder
                            if (adapterPosition != RecyclerView.NO_POSITION) {
                                locationText.text = text
                            }
                        }
                    } else {
                        locationText.text = "Location unavailable"
                    }
                } catch (e: Exception) {
                    locationText.text = "Location unavailable"
                    lastUpdatedText.isVisible = false
                }
            } else {
                locationText.text = "Location unavailable"
                locationText.alpha = 0.75f
                lastUpdatedText.isVisible = false
            }
        }

        fun cancelJob() {
            bindJob?.cancel()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_location, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.cancelJob()
    }

    class DiffCallback : DiffUtil.ItemCallback<StudentLocationInfo>() {
        override fun areItemsTheSame(oldItem: StudentLocationInfo, newItem: StudentLocationInfo): Boolean {
            return oldItem.studentId == newItem.studentId
        }

        override fun areContentsTheSame(oldItem: StudentLocationInfo, newItem: StudentLocationInfo): Boolean {
            return oldItem == newItem
        }
    }
}
