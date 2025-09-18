package com.example.terraconnection.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.adapters.StudentLocationAdapter
import com.example.terraconnection.adapters.StudentLocationInfo
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClassLocationActivity : AppCompatActivity() {
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
    private lateinit var adapter: StudentLocationAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var activeUsersCount: TextView
    private lateinit var connectionStatus: TextView
    private lateinit var emptyState: android.view.View
    private val studentLocations = mutableMapOf<String, StudentLocationInfo>()
    private val reconnectRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing) {
                setupWebSocket()
                reconnectHandler.postDelayed(this, 5000) // Try reconnecting every 5 seconds
            }
        }
    }

    companion object {
        private const val WEBSOCKET_URL = "ws://10.0.2.2:3000/ws"
        private const val BASE_URL = "http://10.0.2.2:3000"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_location)

        classId = intent.getStringExtra("classId") ?: return

        initializeViews()
        setupRecyclerView()
        setupWebSocket()
        fetchInitialLocations()
    }

    private fun initializeViews() {
        activeUsersCount = findViewById(R.id.activeUsersCount)
        connectionStatus = findViewById(R.id.connectionStatus)
        recyclerView = findViewById(R.id.studentsRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        
        // Setup back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = StudentLocationAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
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
                
                runOnUiThread {
                    connectionStatus.text = "Connected"
                }
                
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
                                updateStudentLocation(
                                    json.getString("studentId"),
                                    json.getString("studentName"),
                                    json.optString("generalArea"),
                                    json.optDouble("latitude"),
                                    json.optDouble("longitude"),
                                    json.optString("profilePicture"),
                                    json.getString("role"),
                                    json.optString("timestamp")
                                )
                            }
                            "stop-sharing" -> {
                                removeStudentLocation(json.getString("studentId"))
                            }
                            "activeUsers" -> {
                                val count = json.getInt("count")
                                activeUsersCount.text = count.toString()
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
                    connectionStatus.text = "Disconnected"
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
                runOnUiThread {
                    connectionStatus.text = "Disconnected"
                }
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
                            // Clear existing locations first
                            studentLocations.clear()
                            
                            val currentTimeMillis = System.currentTimeMillis()
                            for (i in 0 until locations.length()) {
                                val location = locations.getJSONObject(i)
                                if (location.has("latitude") && 
                                    location.has("longitude") && 
                                    location.has("timestamp")) {
                                    
                                    // Parse the timestamp from the server
                                    val timestamp = location.getString("timestamp")
                                    val locationTime = java.time.Instant.parse(timestamp).toEpochMilli()
                                    
                                    // Only show locations updated in the last minute
                                    if (currentTimeMillis - locationTime <= 1 * 60 * 1000) {
                                        updateStudentLocation(
                                            location.getString("studentId"),
                                            location.getString("studentName"),
                                            location.optString("generalArea"),
                                            location.getDouble("latitude"),
                                            location.getDouble("longitude"),
                                            location.optString("profilePicture"),
                                            location.getString("role"),
                                            timestamp
                                        )
                                    }
                                }
                            }
                            
                            updateUI()
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

    private fun updateStudentLocation(
        studentId: String,
        studentName: String,
        generalArea: String?,
        latitude: Double?,
        longitude: Double?,
        profilePictureUrl: String?,
        role: String = "student",
        timestamp: String?
    ) {
        val locationInfo = StudentLocationInfo(
            studentId = studentId,
            studentName = studentName,
            profilePicture = profilePictureUrl,
            role = role,
            generalArea = generalArea,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        
        studentLocations[studentId] = locationInfo
        updateUI()
    }

    private fun removeStudentLocation(studentId: String) {
        studentLocations.remove(studentId)
        updateUI()
    }

    private fun updateUI() {
        val locationsList = studentLocations.values.toList()
        adapter.submitList(locationsList)
        
        // Update empty state
        if (locationsList.isEmpty()) {
            recyclerView.visibility = android.view.View.GONE
            emptyState.visibility = android.view.View.VISIBLE
        } else {
            recyclerView.visibility = android.view.View.VISIBLE
            emptyState.visibility = android.view.View.GONE
        }
        
        // Update active users count
        activeUsersCount.text = locationsList.size.toString()
    }

    private fun updateConnectionStatus(status: String) {
        connectionStatus.text = status
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Activity destroyed")
        reconnectHandler.removeCallbacks(reconnectRunnable)
    }
} 