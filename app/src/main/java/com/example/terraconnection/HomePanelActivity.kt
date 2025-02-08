package com.example.terraconnection

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.databinding.ActivityHomePanelBinding

class HomePanelActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomePanelBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomePanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val profileIconButton: ImageButton = binding.studentIcon

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navigate to the student list
        binding.subjectCard.setOnClickListener {
            val intent = Intent(this, ListStudentActivity::class.java)
            startActivity(intent)
        }

        // Navigate to profile
        profileIconButton.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        // 🚀 **Navigate to EventListActivity when clicking Calendar Log**
        binding.calendarLog.setOnClickListener {
            val intent = Intent(this, EventListActivity::class.java)
            startActivity(intent)
        }
    }
}
