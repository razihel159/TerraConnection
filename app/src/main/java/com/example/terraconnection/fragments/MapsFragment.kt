package com.example.terraconnection.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.adapters.ClassListAdapter
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.dialogs.LocationSharingDialog
import com.example.terraconnection.models.ClassItem
import com.example.terraconnection.utils.LocationFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MapsFragment : Fragment() {
    private val apiService = RetrofitClient.apiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClassListAdapter
    private var webSocket: WebSocket? = null
    private val handler = Handler(Looper.getMainLooper())
    private val reconnectRunnable = Runnable { connectWebSocket() }
    private val pingRunnable = object : Runnable {
        override fun run() {
            val pingMessage = JSONObject().apply {
                put("type", "ping")
            }.toString()
            webSocket?.send(pingMessage)
            handler.postDelayed(this, 30000) // Send ping every 30 seconds
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.classRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = ClassListAdapter { classItem ->
            showLocationSharingDialog(classItem)
        }
        recyclerView.adapter = adapter

        // Initialize empty state
        val emptyState = view.findViewById<View>(R.id.emptyState)

        // Update adapter to handle empty state
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                checkEmpty()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                checkEmpty()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                checkEmpty()
            }

            private fun checkEmpty() {
                val isEmpty = adapter.itemCount == 0
                recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            }
        })

        fetchClasses()
    }

    private fun connectWebSocket() {
        val token = SessionManager.getToken(requireContext()) ?: return
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // Disable read timeout for WebSocket
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS) // Enable OkHttp's built-in ping
            .build()
            
        val request = Request.Builder()
            .url("ws://10.0.2.2:3000/ws")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connection opened")
                handler.removeCallbacks(reconnectRunnable)
                handler.removeCallbacks(pingRunnable)
                handler.post(pingRunnable)
                
                // Join all classes when WebSocket connects
                adapter.currentList.forEach { classItem ->
                    val joinMessage = JSONObject().apply {
                        put("type", "join-class")
                        put("classId", classItem.id)
                    }.toString()
                    webSocket.send(joinMessage)
                    Log.d("WebSocket", "Joining class: ${classItem.id}")
                }

                // Request initial active users count for each class
                adapter.currentList.forEach { classItem ->
                    val requestMessage = JSONObject().apply {
                        put("type", "get-active-users")
                        put("classId", classItem.id)
                    }.toString()
                    webSocket.send(requestMessage)
                    Log.d("WebSocket", "Requesting active users for class: ${classItem.id}")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WebSocket", "Received message: $text")
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "activeUsers" -> {
                            val classId = json.getString("classId")
                            val count = json.getInt("count")
                            Log.d("WebSocket", "Received active users update - Class: $classId, Count: $count")
                            activity?.runOnUiThread {
                                adapter.updateActiveUsers(classId, count)
                            }
                        }
                        "pong" -> {
                            Log.d("WebSocket", "Received pong from server")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Error processing message: ${e.message}")
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection failed: ${t.message}")
                t.printStackTrace()
                handler.removeCallbacks(pingRunnable)
                if (isAdded) {
                    handler.postDelayed(reconnectRunnable, 5000)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Connection closed: $reason")
                handler.removeCallbacks(pingRunnable)
                if (isAdded) {
                    handler.postDelayed(reconnectRunnable, 5000)
                }
            }
        })
    }

    private fun fetchClasses() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(requireContext())?.let { "Bearer $it" }
                    ?: throw Exception("No authentication token found")

                val role = SessionManager.getRole(requireContext())
                val response = if (role == "professor") {
                    apiService.getProfessorSchedule(token)
                } else {
                    apiService.getStudentSchedule(token)
                }

                if (response.isSuccessful) {
                    val schedules = response.body()?.schedule ?: emptyList()
                    val classes = schedules.map { schedule ->
                        ClassItem(
                            id = schedule.id.toString(),
                            name = "${schedule.className} (${schedule.classCode})"
                        )
                    }

                    withContext(Dispatchers.Main) {
                        adapter.submitList(classes)
                        // Connect WebSocket after getting class list
                        connectWebSocket()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to load classes: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showLocationSharingDialog(classItem: ClassItem) {
        LocationSharingDialog.newInstance(classItem.id, classItem.name)
            .show(childFragmentManager, "location_sharing")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(reconnectRunnable)
        handler.removeCallbacks(pingRunnable)
        webSocket?.close(1000, null)
    }
}
