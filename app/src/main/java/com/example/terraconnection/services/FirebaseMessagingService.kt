package com.example.terraconnection.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.terraconnection.R
import com.example.terraconnection.TerraApplication
import com.example.terraconnection.activities.LoginPageActivity
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.FcmTokenRequest
import com.example.terraconnection.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TerraPushMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "Received message from: ${message.from}")
        Log.d("FCM", "Message data payload: ${message.data}")
        
        // Handle notification payload
        message.notification?.let { notification ->
            Log.d("FCM", "Message Notification Body: ${notification.body}")
            showNotification(notification.title ?: "Class Notification", notification.body ?: "New notification")
            return
        }

        // Handle data payload
        if (message.data.isNotEmpty()) {
            Log.d("FCM", "Message Data Body: ${message.data["body"]}")
            val title = message.data["title"] ?: "Class Notification"
            val body = message.data["body"] ?: message.data["message"] ?: "You have a new notification"
            showNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token received: $token")
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        Log.d("FCM", "Attempting to send token to server: $token")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authToken = SessionManager.getToken(applicationContext)?.let { "Bearer $it" }
                if (authToken == null) {
                    Log.e("FCM", "No auth token available")
                    return@launch
                }

                Log.d("FCM", "Sending FCM token to server...")
                val response = RetrofitClient.apiService.updateFcmToken(
                    authToken,
                    FcmTokenRequest(token)
                )

                if (response.isSuccessful) {
                    Log.d("FCM", "Successfully updated FCM token on server")
                } else {
                    Log.e("FCM", "Failed to update token: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error updating token: ${e.message}", e)
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        Log.d("FCM", "Creating notification - Title: $title, Message: $message")
        
        val intent = Intent(this, LoginPageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, TerraApplication.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notify)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        Log.d("FCM", "Notification displayed with ID: $notificationId")
    }
}