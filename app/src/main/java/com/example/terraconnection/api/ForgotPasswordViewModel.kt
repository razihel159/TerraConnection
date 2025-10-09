package com.example.terraconnection.api

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.terraconnection.data.ForgotPasswordRequestResponse
import com.example.terraconnection.data.ForgotPasswordVerifyResponse
import com.example.terraconnection.data.MessageResponse
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _resetRequestState = MutableLiveData<Result<ForgotPasswordRequestResponse>>()
    val resetRequestState: LiveData<Result<ForgotPasswordRequestResponse>> get() = _resetRequestState

    private val _otpVerificationState = MutableLiveData<Result<ForgotPasswordVerifyResponse>>()
    val otpVerificationState: LiveData<Result<ForgotPasswordVerifyResponse>> get() = _otpVerificationState

    private val _passwordResetState = MutableLiveData<Result<MessageResponse>>()
    val passwordResetState: LiveData<Result<MessageResponse>> get() = _passwordResetState

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun requestPasswordReset(email: String) {
        _isLoading.value = true
        viewModelScope.launch {
            _resetRequestState.value = repository.requestPasswordReset(email)
            _isLoading.value = false
        }
    }

    fun verifyOtp(email: String, otp: String) {
        _isLoading.value = true
        viewModelScope.launch {
            _otpVerificationState.value = repository.verifyPasswordReset(email, otp)
            _isLoading.value = false
        }
    }

    fun resetPassword(email: String, resetToken: String, password: String) {
        _isLoading.value = true
        viewModelScope.launch {
            _passwordResetState.value = repository.completePasswordReset(email, resetToken, password)
            _isLoading.value = false
        }
    }
}
