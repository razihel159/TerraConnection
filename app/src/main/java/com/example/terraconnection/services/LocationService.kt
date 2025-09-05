package com.example.terraconnection.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.activities.HomePanelActivity
import com.example.terraconnection.utils.LocationFormatter
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import android.content.pm.ServiceInfo
import android.app.PendingIntent

class LocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val CHANNEL_ID = "LocationServiceChannel"
    private val NOTIFICATION_ID = 1
    private val client = OkHttpClient()
    private var classId: String? = null
    private var className: String? = null
    private var isLocationUpdatesStarted = false

    companion object {
        private const val PREFS_NAME = "LocationServicePrefs"
        private const val KEY_ACTIVE_CLASS = "active_sharing_class"
        private const val KEY_ACTIVE_CLASS_NAME = "active_sharing_class_name"
        private const val ACTION_STOP_SHARING = "com.example.terraconnection.STOP_SHARING"

        fun getActiveClass(context: Context): Pair<String?, String?> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val classId = prefs.getString(KEY_ACTIVE_CLASS, null)
            val className = prefs.getString(KEY_ACTIVE_CLASS_NAME, null)
            return Pair(classId, className)
        }

        fun isSharing(context: Context, classId: String): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val activeClassId = prefs.getString(KEY_ACTIVE_CLASS, null)
            
            // Check if service is actually running
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            val isServiceRunning = runningServices.any { 
                it.service.className == LocationService::class.java.name 
            }
            
            // If service is not running, clear the shared preferences
            if (!isServiceRunning) {
                clearSharingState(context)
                return false
            }
            
            return activeClassId == classId
        }
        
        fun clearSharingState(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SHARING -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // If service is already running with a class, don't restart it
        if (isLocationUpdatesStarted) {
            return START_STICKY
        }

        classId = intent?.getStringExtra("classId")
        className = intent?.getStringExtra("className")
        
        if (classId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!hasRequiredPermissions()) {
            android.util.Log.e("LocationService", "Missing required permissions")
            stopSelf()
            return START_NOT_STICKY
        }

        // Save active class info
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_ACTIVE_CLASS, classId)
            putString(KEY_ACTIVE_CLASS_NAME, className)
            apply()
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        setupLocationUpdates()
        return START_STICKY
    }

    private fun setupLocationUpdates() {
        if (isLocationUpdatesStarted) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationUpdate(location)
                }
            }
        }
        
        requestLocationUpdates()
        isLocationUpdatesStarted = true
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()

        try {
            locationCallback?.let { callback ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            }
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun sendLocationUpdate(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(this@LocationService) ?: ""
                android.util.Log.d("LocationService", "Token length: ${token.length}")
                
                if (token.isBlank()) {
                    android.util.Log.e("LocationService", "No auth token available")
                    stopSelf()
                    return@launch
                }

                // Convert GPS coordinates to general area name for privacy
                val generalArea = LocationFormatter.getGeneralAreaName(
                    this@LocationService.applicationContext,
                    location.latitude,
                    location.longitude
                ) ?: "Unknown Area"

                android.util.Log.d("LocationService", "Converted location to general area: $generalArea")

                val json = JSONObject().apply {
                    put("generalArea", generalArea)
                    put("classId", classId)
                    put("timestamp", System.currentTimeMillis())
                    // Still include coordinates for backend processing but they won't be displayed
                    put("latitude", location.latitude)
                    put("longitude", location.longitude)
                }

                android.util.Log.d("LocationService", "Sending location update with general area: $generalArea")

                val request = Request.Builder()
                    .url("https://terraconnection.online/api/location/update")
                    .addHeader("Authorization", "Bearer $token")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        android.util.Log.e("LocationService", "Failed to send location update", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string()
                        android.util.Log.d("LocationService", "Response code: ${response.code}, body: $responseBody")
                        
                        if (!response.isSuccessful) {
                            android.util.Log.e("LocationService", "Server error: ${response.code} - $responseBody")
                            if (response.code == 401 || response.code == 403) {
                                stopSelf()
                            }
                        }
                        response.close()
                    }
                })
            } catch (e: Exception) {
                android.util.Log.e("LocationService", "Error sending location update", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        // Create an explicit intent for the HomePanelActivity
        val activityIntent = Intent(this, HomePanelActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Create a stop action intent
        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP_SHARING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sharing Location")
            .setContentText("Sharing location with ${className ?: "class"}")
            .setSmallIcon(R.drawable.ic_logo)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Sharing", stopPendingIntent)
            .build()
    }

    private fun hasRequiredPermissions(): Boolean {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasForegroundServicePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return hasLocationPermission && hasForegroundServicePermission
        }

        return hasLocationPermission
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        isLocationUpdatesStarted = false
        
        // Clear active class info
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
} 