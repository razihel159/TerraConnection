package com.example.terraconnection.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    private var currentFilter = "All"

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

        setupBackButton()
        setupFilterSpinner()
        setupRecyclerView()
        setupSwipeRefresh()
        fetchNotifications()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupFilterSpinner() {
        val filters = arrayOf("All", "Unread", "Read")
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            filters
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        
        binding.filterSpinner.apply {
            this.adapter = adapter
            setPopupBackgroundResource(R.color.violet)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    currentFilter = filters[position]
                    filterNotifications()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(notifications, object : NotificationAdapter.NotificationListener {
            override fun onNotificationClicked(notification: Notification) {
                markAsRead(notification)
            }
        })
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = notificationAdapter
        }
    }

    private fun filterNotifications() {
        val filteredList = when (currentFilter) {
            "Unread" -> notifications.filter { !it.is_read }
            "Read" -> notifications.filter { it.is_read }
            else -> notifications
        }
        notificationAdapter.updateNotifications(filteredList)
        updateEmptyState(filteredList.isEmpty())
    }

    private fun markAsRead(notification: Notification) {
        if (!notification.is_read) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val token = SessionManager.getToken(this@NotificationActivity)?.let { "Bearer $it" }
                        ?: throw Exception("No authentication token found")

                    val response = apiService.markNotificationAsRead(token, notification.id)
                    
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            notification.is_read = true
                            notificationAdapter.notifyDataSetChanged()
                            filterNotifications()
                        } else {
                            Toast.makeText(
                                this@NotificationActivity,
                                "Failed to mark notification as read",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NotificationActivity", "Error marking notification as read: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@NotificationActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            fetchNotifications()
        }
    }

    private fun fetchNotifications() {
        binding.swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken(this@NotificationActivity)?.let { "Bearer $it" }
                    ?: throw Exception("No authentication token found")

                val response = apiService.getNotifications(token)
                
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    if (response.isSuccessful) {
                        notifications.clear()
                        response.body()?.notifications?.let {
                            notifications.addAll(it)
                            filterNotifications()
                        }
                    } else {
                        Toast.makeText(
                            this@NotificationActivity,
                            "Failed to fetch notifications",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationActivity", "Error fetching notifications: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        this@NotificationActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
