package com.example.terraconnection.api

import android.util.Log
import com.example.terraconnection.data.LoginRequest
import com.example.terraconnection.data.LoginResponse
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
                    response.errorBody()?.string()?.let { Log.e("MyTag", it) }
                    Result.failure(Exception("Login failed: ${response.errorBody()?.string()}"))
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
}
