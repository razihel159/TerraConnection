package com.example.terraconnection.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.terraconnection.R
import com.example.terraconnection.SessionManager
import com.example.terraconnection.adapters.NotificationAdapter
import com.example.terraconnection.api.RetrofitClient
import com.example.terraconnection.data.Notification
import com.example.terraconnection.databinding.ActivityNotificationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()
    private val apiService = RetrofitClient.apiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupSwipeRefresh()
        fetchNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(notifications)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = notificationAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchNotifications()
        }
    }

    private fun fetchNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(this@NotificationActivity)?.let { "Bearer $it" }
                    ?: throw Exception("No authentication token found")

                val response = apiService.getNotifications(token)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val notificationsResponse = response.body()
                        notifications.clear()
                        notificationsResponse?.notifications?.let {
                            notifications.addAll(it)
                        }
                        notificationAdapter.notifyDataSetChanged()

                        // Show empty state if no notifications
                        binding.emptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility = if (notifications.isNotEmpty()) View.VISIBLE else View.GONE
                    } else {
                        Toast.makeText(
                            this@NotificationActivity,
                            "Failed to fetch notifications",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                Log.e("NotificationActivity", "Error fetching notifications: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NotificationActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }
}
