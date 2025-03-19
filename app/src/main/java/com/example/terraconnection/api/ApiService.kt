package com.example.terraconnection.api

import com.example.terraconnection.data.ClassSchedule
import com.example.terraconnection.data.LoginRequest
import com.example.terraconnection.data.LoginResponse
import com.example.terraconnection.data.Schedule
import com.example.terraconnection.data.ScheduleResponse
import com.example.terraconnection.data.User
import com.example.terraconnection.data.StudentAttendanceResponse
import com.example.terraconnection.data.ProfessorAttendanceResponse
import com.example.terraconnection.data.ClassEnrollmentResponse
import com.example.terraconnection.data.NotificationRequest
import com.example.terraconnection.data.NotificationResponse
import com.example.terraconnection.data.FcmTokenRequest
import com.example.terraconnection.data.MessageResponse
import com.example.terraconnection.data.NotificationsResponse
import com.example.terraconnection.data.AttendanceLog
import com.example.terraconnection.data.GPSLocation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<User>

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
    ): Response<ProfessorAttendanceResponse>

    @GET("api/student/attendance")
    suspend fun getStudentAttendance(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Response<StudentAttendanceResponse>

    @GET("api/professor/class-enrollment")
    suspend fun getClassEnrollment(
        @Header("Authorization") token: String,
        @Query("classId") classId: String
    ): Response<ClassEnrollmentResponse>

    @POST("api/professor/notification")
    suspend fun sendNotification(
        @Header("Authorization") token: String,
        @Body notification: NotificationRequest
    ): Response<NotificationResponse>

    @POST("api/user/fcm-token")
    suspend fun updateFcmToken(
        @Header("Authorization") token: String,
        @Body request: FcmTokenRequest
    ): Response<Unit>

    @GET("api/student/notifications")
    suspend fun getNotifications(@Header("Authorization") token: String): Response<NotificationsResponse>

    @POST("api/student/notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Header("Authorization") token: String,
        @Path("id") notificationId: Int
    ): Response<MessageResponse>

    @GET("api/guardian/linked-students")
    suspend fun getLinkedStudents(
        @Header("Authorization") token: String
    ): Response<LinkedStudentsResponse>

    @GET("api/guardian/child-status/{studentId}")
    suspend fun getChildStatus(
        @Header("Authorization") token: String,
        @Path("studentId") studentId: String
    ): Response<ChildStatusResponse>
}

data class LinkedStudentsResponse(
    val students: List<StudentStatus>
)

data class StudentStatus(
    val id: Int,
    val first_name: String,
    val last_name: String,
    val school_id: String,
    val onCampus: Boolean,
    val lastLog: AttendanceLog?,
    val lastGPS: GPSLocation?
)

data class ChildStatusResponse(
    val studentId: String,
    val onCampus: Boolean,
    val lastLog: AttendanceLog?,
    val lastGPS: GPSLocation?
)
