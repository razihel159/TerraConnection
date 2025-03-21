package com.example.terraconnection.data

data class LoginResponse(
    val message: String,
    val token: String?,
    val user: User?,
    val otpRequired: Boolean = false,
    val tempToken: String? = null
)

data class OtpResponse(
    val message: String,
    val otpRequired: Boolean,
    val tempToken: String?
)

data class OtpVerificationRequest(
    val otp: String,
    val tempToken: String
)

data class OtpVerificationResponse(
    val message: String,
    val token: String,
    val user: User
) 