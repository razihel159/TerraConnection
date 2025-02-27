package com.example.terraconnection.data
import kotlinx.parcelize.Parcelize

import android.os.Parcelable

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val message: String,
    val role: String
)

data class User(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val email: String,
    val school_id: String?
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
    val onCampus: Boolean
) : Parcelable

data class Notification(
    val message: String
)



