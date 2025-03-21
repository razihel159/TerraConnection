package com.example.terraconnection.api

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.terraconnection.data.LoginResponse
import com.example.terraconnection.data.OtpVerificationResponse
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginResult = MutableLiveData<Result<LoginResponse>>()
    val loginResult: LiveData<Result<LoginResponse>> get() = _loginResult

    private val _otpVerificationResult = MutableLiveData<Result<OtpVerificationResponse>>()
    val otpVerificationResult: LiveData<Result<OtpVerificationResponse>> get() = _otpVerificationResult

    fun login(email: String, password: String) {
        Log.e("MyTag", "$email, $password")
        viewModelScope.launch {
            _loginResult.value = repository.login(email, password)
        }
    }

    fun verifyOtp(tempToken: String, otp: String) {
        Log.d("OTP", "Starting OTP verification with tempToken: ${tempToken.take(10)}... and OTP: $otp")
        viewModelScope.launch {
            try {
                _otpVerificationResult.value = repository.verifyOtp(tempToken, otp)
                Log.d("OTP", "OTP verification completed with result: ${_otpVerificationResult.value}")
            } catch (e: Exception) {
                Log.e("OTP", "Error during OTP verification", e)
                _otpVerificationResult.value = Result.failure(e)
            }
        }
    }
}
