package com.example.terraconnection

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class TerraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d("Firebase", "Firebase initialized successfully")
            
            // Get FCM token
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("Firebase", "FCM Token: $token")
                    } else {
                        Log.e("Firebase", "Failed to get FCM token", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.e("Firebase", "Failed to initialize Firebase", e)
        }

        createNotificationChannel()
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