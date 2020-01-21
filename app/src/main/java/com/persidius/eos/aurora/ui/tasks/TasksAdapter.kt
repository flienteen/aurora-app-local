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
import com.persidius.eos.aurora.database.entities.Recipient
import com.persidius.eos.aurora.database.entities.Uat
import com.persidius.eos.aurora.databinding.RecipientSearchResultItemBinding
import com.persidius.eos.aurora.util.StreamColors

class TasksAdapter(private var data: List<Triple<Recipient, Uat, Loc>> = listOf(), private val itemClickListener: (Recipient) -> Unit) : RecyclerView.Adapter<TasksAdapter.ResultViewHolder>() {

    inner class ResultViewHolder(val binding: RecipientSearchResultItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Triple<Recipient, Uat, Loc>, itemClickListener: (Recipient) -> Unit) {
            binding.color = ColorStateList.valueOf(StreamColors.from(item.first.stream).color.toArgb())
            binding.recipient = item.first
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
        val binding = RecipientSearchResultItemBinding.inflate(inflater, parent, false)
        return ResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val item = data[position]
        holder.bind(item, itemClickListener)
    }

    override fun getItemCount() = data.size

    fun setData(newData: List<Triple<Recipient, Uat, Loc>>) {
        this.data = newData
        notifyDataSetChanged()
    }


}