package com.example.terraconnection.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.activities.LoginPageActivity
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias Inflate<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchUsers(view)
        view.findViewById<Button>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun fetchUsers(view: View) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token: String? = SessionManager.getToken(requireContext())
                Log.e("TOKEN", "Token being sent: $token")

                if (token.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No token found, please log in again", Toast.LENGTH_SHORT).show()
                        logout()
                    }
                    return@launch
                }

                val response = RetrofitClient.apiService.getUsers("Bearer $token")

                if (response.isSuccessful) {
                    val user: User? = response.body()
                    if (user != null) {
                        withContext(Dispatchers.Main) {
                            view.findViewById<TextView>(R.id.firstNameText).text = user.first_name
                            view.findViewById<TextView>(R.id.lastNameText).text = user.last_name
                            view.findViewById<TextView>(R.id.emailText).text = user.email
                            view.findViewById<TextView>(R.id.studentIdText).text = user.school_id ?: "N/A"
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Failed response: ${response.code()} - ${response.errorBody()?.string()}")
                    if (response.code() == 401) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                            logout()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Logout")
        builder.setMessage("Are you sure you want to log out?")
        builder.setPositiveButton("No") { dialog, _ -> dialog.dismiss() }
        builder.setNegativeButton("Yes") { _, _ -> logout() }
        builder.show()
    }

    private fun logout() {
        SessionManager.clearSession(requireContext())
        val intent = Intent(requireContext(), LoginPageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
