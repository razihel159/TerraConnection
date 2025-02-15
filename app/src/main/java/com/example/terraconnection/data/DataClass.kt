package com.example.terraconnection.data

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
    val subjectCode: String,
    val subjectName: String,
    val room: String,
    val time: String
)

data class ClassSchedule(
    val subject: String,
    val room: String,
    val time: String,
    val date: String
)


