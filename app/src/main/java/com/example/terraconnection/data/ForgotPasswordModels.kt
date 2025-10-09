package com.example.terraconnection.data

data class ForgotPasswordRequest(
    val email: String
)

data class ForgotPasswordRequestResponse(
    val message: String?,
    val emailMasked: String?,
    val expiresAt: String?
)

data class ForgotPasswordVerifyRequest(
    val email: String,
    val otp: String
)

data class ForgotPasswordVerifyResponse(
    val message: String?,
    val resetToken: String,
    val expiresAt: String?
)

data class ForgotPasswordResetRequest(
    val email: String,
    val resetToken: String,
    val password: String
)
