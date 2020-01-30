package com.persidius.eos.aurora.ui.tasks

import android.content.res.ColorStateList
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.databinding.adapters.Converters.convertColorToColorStateList
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.persidius.eos.aurora.database.entities.Loc
import com.persidius.eos.aurora.database.entities.Task
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.databinding.TaskSearchResultItemBinding
import com.persidius.eos.aurora.util.StreamColors

class TasksAdapter(private var data: List<Triple<Task, Uat, Loc>> = listOf(), private val itemClickListener: (Task) -> Unit) : RecyclerView.Adapter<TasksAdapter.ResultViewHolder>() {

    inner class ResultViewHolder(val binding: TaskSearchResultItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Triple<Task, Uat, Loc>, itemClickListener: (Task) -> Unit) {
            binding.task = item.first
            binding.uat = item.second
            binding.loc = item.third

            binding.root.setOnClickListener {
                itemClickListener(item.first)
            }
            binding.root.isClickable = true

            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TaskSearchResultItemBinding.inflate(inflater, parent, false)
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item, itemClickListener)
    }

    override fun getItemCount() = data.size

    fun setData(newData: List<Triple<Task, Uat, Loc>>) {
        this.data = newData
        notifyDataSetChanged()
    }


}