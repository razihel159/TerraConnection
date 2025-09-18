package com.example.terraconnection.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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
import com.example.terraconnection.api.RetrofitClient
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

class GlobalLocationService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val CHANNEL_ID = "GlobalLocationServiceChannel"
    private val NOTIFICATION_ID = 2
    private val client = OkHttpClient()
    private var userClasses = listOf<String>()

    companion object {
        private const val PREFS_NAME = "GlobalLocationServicePrefs"
        private const val KEY_IS_RUNNING = "is_running"

        fun isRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
            
            // Verify service is actually running
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            val serviceRunning = runningServices.any { 
                it.service.className == GlobalLocationService::class.java.name 
            }
            
            if (!serviceRunning && isRunning) {
                // Service stopped but prefs still say running - clear it
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_IS_RUNNING, false)
                    .apply()
                return false
            }
            
            return serviceRunning
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if user is a guardian - guardians shouldn't share their location
        val role = SessionManager.getRole(this)
        if (role == "guardian") {
            android.util.Log.d("GlobalLocationService", "Guardians don't share location, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        if (!hasRequiredPermissions()) {
            android.util.Log.e("GlobalLocationService", "Missing required permissions")
            stopSelf()
            return START_NOT_STICKY
        }

        // Mark as running
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_RUNNING, true)
            .apply()

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        fetchUserClasses()
        setupLocationUpdates()
        return START_STICKY
    }

    private fun fetchUserClasses() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(this@GlobalLocationService)?.let { "Bearer $it" }
                    ?: return@launch
                
                val role = SessionManager.getRole(this@GlobalLocationService)
                val response = if (role == "professor") {
                    RetrofitClient.apiService.getProfessorSchedule(token)
                } else {
                    RetrofitClient.apiService.getStudentSchedule(token)
                }

                if (response.isSuccessful) {
                    val schedules = response.body()?.schedule ?: emptyList()
                    userClasses = schedules.map { it.id.toString() }
                    android.util.Log.d("GlobalLocationService", "Loaded ${userClasses.size} classes")
                }
            } catch (e: Exception) {
                android.util.Log.e("GlobalLocationService", "Error fetching classes", e)
            }
        }
    }

    private fun setupLocationUpdates() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationUpdate(location)
                }
            }
        }
        
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000) // Every 30 seconds
            .setMinUpdateIntervalMillis(15000) // Minimum 15 seconds
            .build()

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
                locationCallback?.let { callback ->
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        callback,
                        Looper.getMainLooper()
                    )
                }
            }
        } catch (e: SecurityException) {
            android.util.Log.e("GlobalLocationService", "Security exception", e)
            stopSelf()
        }
    }

    private fun sendLocationUpdate(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(this@GlobalLocationService) ?: ""
                if (token.isBlank()) {
                    android.util.Log.e("GlobalLocationService", "No auth token available")
                    stopSelf()
                    return@launch
                }

                // Check student sharing preferences
                val prefs = getSharedPreferences("StudentLocationPrefs", Context.MODE_PRIVATE)
                val shareWithClasses = prefs.getBoolean("share_with_classes", true)
                val shareWithGuardian = prefs.getBoolean("share_with_guardian", true)
                
                // Convert GPS to general area
                val generalArea = LocationFormatter.getGeneralAreaName(
                    this@GlobalLocationService.applicationContext,
                    location.latitude,
                    location.longitude
                ) ?: "Unknown Area"

                // Send location update for classes if enabled
                if (shareWithClasses && userClasses.isNotEmpty()) {
                    userClasses.forEach { classId ->
                        val json = JSONObject().apply {
                            put("generalArea", generalArea)
                            put("classId", classId)
                            put("timestamp", System.currentTimeMillis())
                            put("latitude", location.latitude)
                            put("longitude", location.longitude)
                            put("shareType", "classes")
                        }

                        val request = Request.Builder()
                            .url("http://10.0.2.2:3000/api/location/update")
                            .addHeader("Authorization", "Bearer $token")
                            .post(json.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                android.util.Log.e("GlobalLocationService", "Failed to update location for class $classId", e)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                if (!response.isSuccessful) {
                                    android.util.Log.e("GlobalLocationService", "Server error for class $classId: ${response.code}")
                                    if (response.code == 401 || response.code == 403) {
                                        stopSelf()
                                    }
                                }
                                response.close()
                            }
                        })
                    }
                    android.util.Log.d("GlobalLocationService", "Location update sent to ${userClasses.size} classes: $generalArea")
                }
                
                // Send location update for guardian if enabled
                if (shareWithGuardian) {
                    val json = JSONObject().apply {
                        put("generalArea", generalArea)
                        put("timestamp", System.currentTimeMillis())
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                        put("shareType", "guardian")
                    }

                    val request = Request.Builder()
                        .url("http://10.0.2.2:3000/api/location/guardian-update")
                        .addHeader("Authorization", "Bearer $token")
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            android.util.Log.e("GlobalLocationService", "Failed to update guardian location", e)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            if (!response.isSuccessful) {
                                android.util.Log.e("GlobalLocationService", "Server error for guardian update: ${response.code}")
                            }
                            response.close()
                        }
                    })
                    android.util.Log.d("GlobalLocationService", "Location update sent to guardian: $generalArea")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("GlobalLocationService", "Error sending location update", e)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Global Location Service",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Shares your general location with all your classes"
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val activityIntent = Intent(this, HomePanelActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sharing Location")
            .setContentText("Your general location is being shared with your classes")
            .setSmallIcon(R.drawable.ic_location_vector)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        
        // Mark as not running
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_RUNNING, false)
            .apply()
            
        android.util.Log.d("GlobalLocationService", "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
