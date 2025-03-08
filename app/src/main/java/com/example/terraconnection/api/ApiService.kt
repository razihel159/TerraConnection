package com.example.terraconnection.api

import com.example.terraconnection.data.ClassSchedule
import com.example.terraconnection.data.LoginRequest
import com.example.terraconnection.data.LoginResponse
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.data.ScheduleResponse
import com.example.terraconnection.data.User
import com.example.terraconnection.data.AttendanceResponse
import com.example.terraconnection.data.ClassEnrollmentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getUsers(@Header("Authorization") token: String): Response<User>

    @GET("api/student/schedule")
    suspend fun getStudentSchedule(@Header("Authorization") token: String): Response<ScheduleResponse>

    @GET("api/professor/schedule")
    suspend fun getProfessorSchedule(@Header("Authorization") token: String): Response<ScheduleResponse>

    @GET("api/professor/attendance")
    suspend fun getProfessorAttendance(
        @Header("Authorization") token: String,
        @Query("date") date: String,
        @Query("classId") classId: String
    ): Response<AttendanceResponse>

    @GET("api/professor/class-enrollment")
    suspend fun getClassEnrollment(
        @Header("Authorization") token: String,
        @Query("classId") classId: String
    ): Response<ClassEnrollmentResponse>
}
