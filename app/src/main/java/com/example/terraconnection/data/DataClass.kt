package com.example.terraconnection.data

    data class LoginRequest(
        val email: String,
        val password: String
    )

    data class LoginResponse(
        val token: String,
        val message: String
    )

    data class Schedule(
        val subjectCode: String,
        val subjectName: String,
        val room: String,
        val time: String
    )

