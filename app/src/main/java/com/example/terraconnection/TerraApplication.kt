package com.example.terraconnection

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.FcmTokenRequest
import com.example.terraconnection.SessionManager
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TerraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase initialized successfully")
            
            // Get and refresh FCM token
            refreshFCMToken()

        } catch (e: Exception) {
            Log.e("Firebase", "Failed to initialize Firebase", e)
        }

        createNotificationChannel()
    }

    private fun refreshFCMToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("Firebase", "FCM Token: $token")
                    // Send token to server if user is logged in
                    val authToken = SessionManager.getToken(this)
                    if (authToken != null) {
                        sendTokenToServer(token, authToken)
                    }
                } else {
                    Log.e("Firebase", "Failed to get FCM token", task.exception)
                }
            }
    }

    private fun sendTokenToServer(fcmToken: String, authToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("Firebase", "Sending FCM token to server...")
                Log.d("Firebase", "Auth token being used: ${authToken.take(20)}...")
                Log.d("Firebase", "FCM token being sent: ${fcmToken.take(20)}...")
                
                val response = RetrofitClient.apiService.updateFcmToken(
                    "Bearer $authToken",
                    FcmTokenRequest(fcmToken)
                )

                if (response.isSuccessful) {
                    Log.d("Firebase", "Successfully updated FCM token on server")
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("Firebase", "Failed to update token: ${response.code()} - ${response.message()}")
                    Log.e("Firebase", "Error body: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("Firebase", "Error updating token: ${e.message}", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Class Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for class attendance and reminders"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("Firebase", "Notification channel created: $CHANNEL_ID")
        }
    }

    companion object {
        const val CHANNEL_ID = "terra_channel"
    }
} 