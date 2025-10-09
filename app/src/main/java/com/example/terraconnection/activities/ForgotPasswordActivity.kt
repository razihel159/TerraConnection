package com.example.terraconnection.activities

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.terraconnection.ThemeManager
import com.example.terraconnection.api.ForgotPasswordViewModel
import com.example.terraconnection.databinding.ActivityForgotPasswordBinding
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    private var resetToken: String? = null
    private var currentEmail: String? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbarForgotPassword.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnSendResetCode.setOnClickListener {
            binding.tilEmail.error = null
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = getString(com.example.terraconnection.R.string.forgot_password_invalid_email)
                return@setOnClickListener
            }
            currentEmail = email
            resetToken = null
            binding.passwordFieldsContainer.visibility = View.GONE
            binding.tilOtp.error = null
            viewModel.requestPasswordReset(email)
        }

        binding.btnVerifyCode.setOnClickListener {
            val email = currentEmail ?: run {
                Toast.makeText(this, com.example.terraconnection.R.string.forgot_password_invalid_email, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val otp = binding.etOtp.text?.toString()?.trim().orEmpty()
            if (otp.length != 6) {
                binding.tilOtp.error = getString(com.example.terraconnection.R.string.forgot_password_invalid_code)
                return@setOnClickListener
            }
            binding.tilOtp.error = null
            viewModel.verifyOtp(email, otp)
        }

        binding.btnResetPassword.setOnClickListener {
            val email = currentEmail ?: run {
                Toast.makeText(this, com.example.terraconnection.R.string.forgot_password_invalid_email, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = resetToken ?: run {
                Toast.makeText(this, getString(com.example.terraconnection.R.string.forgot_password_invalid_code), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newPassword = binding.etNewPassword.text?.toString()?.trim().orEmpty()
            val confirmPassword = binding.etConfirmPassword.text?.toString()?.trim().orEmpty()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                binding.tilNewPassword.error = getString(com.example.terraconnection.R.string.forgot_password_new_password_hint)
                binding.tilConfirmPassword.error = getString(com.example.terraconnection.R.string.forgot_password_confirm_password_hint)
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                binding.tilConfirmPassword.error = getString(com.example.terraconnection.R.string.forgot_password_password_mismatch)
                return@setOnClickListener
            }

            binding.tilNewPassword.error = null
            binding.tilConfirmPassword.error = null

            viewModel.resetPassword(email, token, newPassword)
        }

        binding.tvResendCode.setOnClickListener {
            if (binding.tvResendCode.isEnabled.not()) return@setOnClickListener
            val email = currentEmail ?: return@setOnClickListener
            viewModel.requestPasswordReset(email)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressForgotPassword.isVisible = isLoading
            binding.btnSendResetCode.isEnabled = !isLoading
            binding.btnVerifyCode.isEnabled = !isLoading
            binding.btnResetPassword.isEnabled = !isLoading && resetToken != null
            binding.tvResendCode.isEnabled = !isLoading && binding.tvResendCode.alpha == 1f
        }

        viewModel.resetRequestState.observe(this) { result ->
            result.onSuccess { response ->
                binding.stepVerificationContainer.visibility = View.VISIBLE
                binding.tvEmailMasked.text = response.emailMasked?.let {
                    getString(com.example.terraconnection.R.string.forgot_password_code_sent_masked, it)
                } ?: currentEmail
                binding.passwordFieldsContainer.visibility = View.GONE
                resetToken = null
                binding.etOtp.text = null
                binding.tvResendCode.isEnabled = false
                binding.tvResendCode.alpha = 0.5f
                startOtpCountdown(response.expiresAt)
                Toast.makeText(
                    this,
                    response.message ?: getString(com.example.terraconnection.R.string.forgot_password_send_code),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                Toast.makeText(
                    this,
                    error.message ?: getString(com.example.terraconnection.R.string.forgot_password_invalid_email),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        viewModel.otpVerificationState.observe(this) { result ->
            result.onSuccess { response ->
                resetToken = response.resetToken
                binding.passwordFieldsContainer.visibility = View.VISIBLE
                binding.btnVerifyCode.isEnabled = false
                binding.tilOtp.error = null
                Toast.makeText(
                    this,
                    getString(com.example.terraconnection.R.string.forgot_password_verify_success),
                    Toast.LENGTH_SHORT
                ).show()
                response.expiresAt?.let { startOtpCountdown(it) }
            }.onFailure { error ->
                binding.tilOtp.error = error.message ?: getString(com.example.terraconnection.R.string.forgot_password_invalid_code)
            }
        }

        viewModel.passwordResetState.observe(this) { result ->
            result.onSuccess { response ->
                Toast.makeText(
                    this,
                    response.message ?: getString(com.example.terraconnection.R.string.forgot_password_reset_success),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }.onFailure { error ->
                binding.tilConfirmPassword.error = error.message
            }
        }
    }

    private fun startOtpCountdown(expiresAtIso: String?) {
        val millisRemaining = calculateMillisUntilExpiry(expiresAtIso)
        countDownTimer?.cancel()

        if (millisRemaining <= 0L) {
            binding.tvCountdown.text = getString(com.example.terraconnection.R.string.forgot_password_code_expired)
            setResendEnabled(true)
            return
        }

        binding.tvCountdown.visibility = View.VISIBLE
        setResendEnabled(false)
        countDownTimer = object : CountDownTimer(millisRemaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val totalSeconds = millisUntilFinished / 1000
                val minutes = (totalSeconds / 60).toInt()
                val seconds = (totalSeconds % 60).toInt()
                binding.tvCountdown.text = getString(
                    com.example.terraconnection.R.string.forgot_password_code_expires_in,
                    minutes,
                    seconds
                )
            }

            override fun onFinish() {
                binding.tvCountdown.text = getString(com.example.terraconnection.R.string.forgot_password_code_expired)
                setResendEnabled(true)
                resetToken = null
                binding.btnVerifyCode.isEnabled = true
            }
        }.start()
    }

    private fun setResendEnabled(enabled: Boolean) {
        binding.tvResendCode.isEnabled = enabled
        binding.tvResendCode.alpha = if (enabled) 1f else 0.5f
    }

    private fun calculateMillisUntilExpiry(expiresAtIso: String?): Long {
        val defaultDuration = DEFAULT_OTP_DURATION_MINUTES * 60 * 1000L
        if (expiresAtIso.isNullOrBlank()) {
            return defaultDuration
        }
        return try {
            val expiryMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant.parse(expiresAtIso).toEpochMilli()
            } else {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                parser.parse(expiresAtIso)?.time
            }
            val remaining = (expiryMillis ?: return defaultDuration) - System.currentTimeMillis()
            if (remaining > 0) remaining else 0L
        } catch (ex: Exception) {
            defaultDuration
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val DEFAULT_OTP_DURATION_MINUTES = 15L
    }
}
