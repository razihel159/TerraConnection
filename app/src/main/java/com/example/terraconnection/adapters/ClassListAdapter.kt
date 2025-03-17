package com.example.terraconnection.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.terraconnection.R
import com.example.terraconnection.models.ClassItem

class ClassListAdapter(
    private val onClassClick: (ClassItem) -> Unit
) : ListAdapter<ClassItem, ClassListAdapter.ClassViewHolder>(ClassDiffCallback()) {

    private val activeUsersMap = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_class, parent, false)
        return ClassViewHolder(view, onClassClick)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classItem = getItem(position)
        holder.bind(classItem, activeUsersMap[classItem.id] ?: 0)
    }

    fun updateActiveUsers(classId: String, count: Int) {
        activeUsersMap[classId] = count
        notifyDataSetChanged() // In a production app, we should use DiffUtil for better performance
    }

    class ClassViewHolder(
        itemView: View,
        private val onClassClick: (ClassItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.classNameTextView)
        private val activeUsersCount: TextView = itemView.findViewById(R.id.activeUsersCount)

        fun bind(classItem: ClassItem, activeUsers: Int) {
            nameTextView.text = classItem.name
            activeUsersCount.text = activeUsers.toString()
            itemView.setOnClickListener { onClassClick(classItem) }
        }
    }

    private class ClassDiffCallback : DiffUtil.ItemCallback<ClassItem>() {
        override fun areItemsTheSame(oldItem: ClassItem, newItem: ClassItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ClassItem, newItem: ClassItem): Boolean {
            return oldItem == newItem
        }
    }
} 