package com.persidius.eos.aurora.ui.recipient

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.persidius.eos.aurora.databinding.TagSlotItemBinding
import com.persidius.eos.aurora.util.Tuple2

class TagListAdapter(context: Context,
                     private val items: List<Tuple2<LiveData<String>, LiveData<Boolean>>>,
                     private val lifecycleOwner: LifecycleOwner,
                     private val onTagClick: (Int) -> Unit = { _ -> Unit
                     }
        ): BaseAdapter() {
    private val layoutInflater = LayoutInflater.from(context)
    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var binding = if (convertView != null) {
            convertView.tag as TagSlotItemBinding
        } else {
            TagSlotItemBinding.inflate(layoutInflater, parent, false)
        }

        binding.root.tag = binding
        binding.lifecycleOwner = lifecycleOwner


        val slot = position + 1
        binding.tagSlot = slot
        binding.tagId = items[position].first
        binding.selected = items[position].second



        binding.root.setOnClickListener {
            Log.d("tag", "click")
            onTagClick(slot)
        }

        return binding.root
    }
}
