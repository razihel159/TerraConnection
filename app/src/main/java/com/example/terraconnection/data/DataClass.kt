package com.example.terraconnection.data
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

data class LoginRequest(
    val email: String,
    val password: String
)

data class User(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val email: String,
    val school_id: String?,
    val profile_picture: String?
)

data class Schedule(
    val id: Int,
    val class_code: String,
    val class_name: String,
    val course: String,
    val year: Int,
    val section: String,
    val room: String,
    val start_time: String,
    val end_time: String,
    val schedule: String,
    val createdAt: String,
    val updatedAt: String
) {
    val classCode: String get() = class_code
    val className: String get() = class_name
    val startTime: String get() = start_time
    val endTime: String get() = end_time
    val scheduleDay: String get() = schedule
}

data class ScheduleResponse(
    val schedule: List<Schedule>
)

data class ClassSchedule(
    val subject: String,
    val room: String,
    val time: String,
    val date: String
)

@Parcelize
data class Student(
    val id: Int,
    val name: String,
    val onCampus: Boolean,
    val role: String,
    val statusIndicator: Int,
    val notifyIcon: Int
) : Parcelable

data class Notification(
    val id: Int,
    val title: String,
    val message: String,
    val class_name: String,
    val class_code: String,
    val sender_name: String,
    val created_at: String,
    var is_read: Boolean = false
)

data class NotificationRequest(
    val classId: String,
    val title: String,
    val message: String
)

data class NotificationResponse(
    val message: String
)

data class NotificationsResponse(
    val notifications: List<Notification>
)

data class StudentAttendanceResponse(
    val attendance: List<AttendanceLog>
)

data class ProfessorAttendanceResponse(
    val date: String,
    val classId: String,
    val attendance: List<StudentAttendance>
)

data class StudentAttendance(
    val studentId: Int,
    val logs: List<AttendanceLog>,
    val lastKnownLocation: GPSLocation?
)

data class AttendanceLog(
    val user_id: Int,
    val timestamp: String,
    val type: String,
    val user: User
)

data class GPSLocation(
    val user_id: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)

data class ClassEnrollmentResponse(
    val enrollments: List<ClassEnrollment>
)

data class ClassEnrollment(
    val studentId: Int,
    val student: EnrolledStudent
)

data class EnrolledStudent(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String
)

data class FcmTokenRequest(
    val fcmToken: String
)

data class MessageResponse(
    val message: String
)



