package com.example.terraconnection.fragments

import android.os.Bundle
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MapsFragment : Fragment() {
    private val apiService = RetrofitClient.apiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ClassListAdapter
    private var webSocket: WebSocket? = null

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

        fetchClasses()
        connectWebSocket()
    }

    private fun connectWebSocket() {
        val token = SessionManager.getToken(requireContext()) ?: return
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("wss://terraconnection.online/ws")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.getString("type")) {
                        "activeUsers" -> {
                            val classId = json.getString("classId")
                            val count = json.getInt("count")
                            activity?.runOnUiThread {
                                adapter.updateActiveUsers(classId, count)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
        webSocket?.close(1000, null)
    }
}
