package com.example.terraconnection.api

import android.util.Log
import com.example.terraconnection.data.ForgotPasswordRequest
import com.example.terraconnection.data.ForgotPasswordRequestResponse
import com.example.terraconnection.data.ForgotPasswordVerifyRequest
import com.example.terraconnection.data.ForgotPasswordVerifyResponse
import com.example.terraconnection.data.ForgotPasswordResetRequest
import com.example.terraconnection.data.LoginRequest
import com.example.terraconnection.data.LoginResponse
import com.example.terraconnection.data.MessageResponse
import com.example.terraconnection.data.OtpVerificationRequest
import com.example.terraconnection.data.OtpVerificationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    val api = RetrofitClient.apiService

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                Log.e("MyTag", "trying")
                val response = api.login(LoginRequest(email, password))
                Log.e("MyTag", response.toString())
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    errorBody?.let { Log.e("MyTag", it) }
                    Result.failure(Exception("Login failed: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e("MyTag", e.toString())
                Result.failure(e)
            }
        }
    }

    suspend fun verifyOtp(tempToken: String, otp: String): Result<OtpVerificationResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.verifyOtp(OtpVerificationRequest(otp = otp, tempToken = tempToken))
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("OTP", "Verification failed with error: $errorBody")
                    Result.failure(Exception("OTP verification failed: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e("OTP", "Verification failed with exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun requestPasswordReset(email: String): Result<ForgotPasswordRequestResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.requestPasswordReset(ForgotPasswordRequest(email = email))
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ForgotPassword", "Reset request failed: $errorBody")
                    Result.failure(Exception(errorBody ?: "Unable to request password reset"))
                }
            } catch (e: Exception) {
                Log.e("ForgotPassword", "Reset request exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun verifyPasswordReset(email: String, otp: String): Result<ForgotPasswordVerifyResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.verifyPasswordReset(
                    ForgotPasswordVerifyRequest(
                        email = email,
                        otp = otp
                    )
                )
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ForgotPassword", "OTP verification failed: $errorBody")
                    Result.failure(Exception(errorBody ?: "Invalid or expired code"))
                }
            } catch (e: Exception) {
                Log.e("ForgotPassword", "OTP verification exception", e)
                Result.failure(e)
            }
        }
    }

    suspend fun completePasswordReset(
        email: String,
        resetToken: String,
        password: String
    ): Result<MessageResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.completePasswordReset(
                    ForgotPasswordResetRequest(
                        email = email,
                        resetToken = resetToken,
                        password = password
                    )
                )
                if (response.isSuccessful) {
                    Result.success(response.body() ?: MessageResponse("Password updated successfully"))
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ForgotPassword", "Password reset failed: $errorBody")
                    Result.failure(Exception(errorBody ?: "Unable to reset password"))
                }
            } catch (e: Exception) {
                Log.e("ForgotPassword", "Password reset exception", e)
                Result.failure(e)
            }
        }
    }
}
