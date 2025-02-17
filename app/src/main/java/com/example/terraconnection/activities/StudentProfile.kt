package com.example.terraconnection.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentProfile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_profile)

        fetchUsers()
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            logout()
        }
    }

    private fun fetchUsers() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token: String? = SessionManager.getToken(this@StudentProfile)
                Log.e("TOKEN", "Token being sent: $token")

                if (token.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@StudentProfile, "No token found, please log in again", Toast.LENGTH_SHORT).show()
                        logout()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getUsers("Bearer $token")

                if (response.isSuccessful) {
                    val user: User? = response.body() // ✅ Ensure it's a single User object

                    if (user != null) { // ✅ Check if user is not null
                        withContext(Dispatchers.Main) {
                            findViewById<TextView>(R.id.firstNameText).text = user.first_name
                            findViewById<TextView>(R.id.lastNameText).text = user.last_name
                            findViewById<TextView>(R.id.emailText).text = user.email
                            findViewById<TextView>(R.id.studentIdText).text = user.school_id ?: "N/A"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@StudentProfile, "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Failed response: ${response.code()} - ${response.errorBody()?.string()}")

                    if (response.code() == 401) { // Token expired
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@StudentProfile, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                            logout()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@StudentProfile, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@StudentProfile, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun logout() {
        SessionManager.clearSession(this)
        val intent = Intent(this, LoginPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
