package com.example.terraconnection.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.services.LocationService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClassLocationActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private var webSocket: WebSocket? = null
    private var classId: String = ""
    private var isReconnecting = false
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private val markers = mutableMapOf<String, Marker?>()
    private val reconnectRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing) {
                setupWebSocket()
                reconnectHandler.postDelayed(this, 5000) // Try reconnecting every 5 seconds
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val WEBSOCKET_URL = "wss://terraconnection.online/ws"
        private const val BASE_URL = "https://terraconnection.online"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_location)

        classId = intent.getStringExtra("classId") ?: return

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkLocationPermission()
        setupWebSocket()
        fetchInitialLocations()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun startLocationService() {
        // Removed as we don't want to start/stop the service from this activity
    }

    private fun setupWebSocket() {
        if (isReconnecting) return

        val token = SessionManager.getToken(this) ?: return

        val request = Request.Builder()
            .url(WEBSOCKET_URL)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isReconnecting = false
                reconnectHandler.removeCallbacks(reconnectRunnable)
                
                webSocket.send(JSONObject().apply {
                    put("type", "join-class")
                    put("classId", classId)
                }.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    runOnUiThread {
                        when (json.getString("type")) {
                            "location-update" -> {
                                updateStudentMarker(
                                    json.getString("studentId"),
                                    json.getString("studentName"),
                                    json.getDouble("latitude"),
                                    json.getDouble("longitude"),
                                    json.optString("profilePicture")
                                )
                            }
                            "stop-sharing" -> {
                                removeStudentMarker(json.getString("studentId"))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                runOnUiThread {
                    if (!isReconnecting) {
                        isReconnecting = true
                        Toast.makeText(
                            this@ClassLocationActivity,
                            "Connection lost. Attempting to reconnect...",
                            Toast.LENGTH_SHORT
                        ).show()
                        reconnectHandler.postDelayed(reconnectRunnable, 1000)
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isFinishing) {
                    isReconnecting = true
                    reconnectHandler.postDelayed(reconnectRunnable, 1000)
                }
            }
        })
    }

    private fun fetchInitialLocations() {
        val token = SessionManager.getToken(this)?.let { "Bearer $it" }
            ?: throw Exception("No authentication token found")

        val request = Request.Builder()
            .url("$BASE_URL/api/location/class/$classId")
            .addHeader("Authorization", token)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@ClassLocationActivity,
                        "Failed to fetch locations: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    try {
                        val json = JSONObject(responseBody)
                        if (!json.getBoolean("success")) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@ClassLocationActivity,
                                    "Failed to fetch locations: ${json.optString("message", "Unknown error")}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@let
                        }

                        val locations = json.getJSONArray("data")
                        runOnUiThread {
                            // Clear existing markers first
                            markers.values.forEach { it?.remove() }
                            markers.clear()
                            
                            val currentTimeMillis = System.currentTimeMillis()
                            for (i in 0 until locations.length()) {
                                val location = locations.getJSONObject(i)
                                if (location.has("latitude") && 
                                    location.has("longitude") && 
                                    location.has("timestamp")) {
                                    
                                    // Parse the timestamp from the server
                                    val timestamp = location.getString("timestamp")
                                    val locationTime = java.time.Instant.parse(timestamp).toEpochMilli()
                                    
                                    // Only show markers for locations updated in the last minute
                                    if (currentTimeMillis - locationTime <= 1 * 60 * 1000) {
                                        updateStudentMarker(
                                            location.getString("studentId"),
                                            location.getString("studentName"),
                                            location.getDouble("latitude"),
                                            location.getDouble("longitude"),
                                            location.optString("profilePicture")
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@ClassLocationActivity,
                                "Error parsing locations: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        })
    }

    private fun createCustomMarker(profilePictureUrl: String?): Bitmap {
        val markerView = LayoutInflater.from(this).inflate(R.layout.custom_marker, null)
        val profileImageView = markerView.findViewById<ImageView>(R.id.profile_picture)

        if (!profilePictureUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load("$BASE_URL$profilePictureUrl")
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .circleCrop()
                .into(profileImageView)
        }

        val width = resources.getDimensionPixelSize(R.dimen.marker_width)
        val height = resources.getDimensionPixelSize(R.dimen.marker_height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.measure(width, height)
        markerView.layout(0, 0, width, height)
        markerView.draw(canvas)
        return bitmap
    }

    private fun updateStudentMarker(
        studentId: String,
        studentName: String,
        latitude: Double,
        longitude: Double,
        profilePictureUrl: String?
    ) {
        val position = LatLng(latitude, longitude)
        val markerOptions = MarkerOptions()
            .position(position)
            .title(studentName)
            .icon(BitmapDescriptorFactory.fromBitmap(createCustomMarker(profilePictureUrl)))

        markers[studentId]?.remove()
        markers[studentId] = map.addMarker(markerOptions)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }

    private fun removeStudentMarker(studentId: String) {
        markers[studentId]?.remove()
        markers.remove(studentId)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (::map.isInitialized) {
                    map.isMyLocationEnabled = true
                }
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required for this feature",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Activity destroyed")
        reconnectHandler.removeCallbacks(reconnectRunnable)
        // Removed stopService call as we don't want to stop the location service
    }
} 