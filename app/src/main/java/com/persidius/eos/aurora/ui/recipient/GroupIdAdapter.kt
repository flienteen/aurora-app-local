package com.persidius.eos.aurora.ui.util

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.persidius.eos.aurora.database.Database


class GroupIdAdapter(private val context: Context,
                          private val resource: Int
): BaseAdapter(), Filterable {

    private val layoutInflater = LayoutInflater.from(context)

    private var items: List<String> = listOf()

    private val filter: GroupIdFilter = GroupIdFilter()

    inner class Validator: AutoCompleteTextView.Validator {

        override fun isValid(text: CharSequence?): Boolean = items.contains(text.toString())

        override fun fixText(invalidText: CharSequence?): CharSequence {
            // see if we can match it against anything
            val match = invalidText.toString().toLowerCase()
            for((index,item) in items.withIndex()) {
                if(item.toLowerCase().startsWith(match)) {
                    return items[index]
                }
            }
            return ""
        }
    }

    fun getValidator() = Validator()

    //// TEXT FILTERING
    private inner class GroupIdFilter: Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()

            if(constraint == null || constraint.isEmpty()) {
                // Stick all results on no constraints
                results.values = items
                results.count = items.size
            } else {
                val term = constraint.toString().toLowerCase()
                val searchResults = Database.groups.search("*$term*")
                    .blockingGet()

                val values = searchResults.take(10).map { g -> g.id }
                results.values = values
                results.count = values.size
            }
            return results
        }

        override fun publishResults(
            constraint: CharSequence?,
            results: FilterResults?
        ) {

            items = (results?.values as List<String>?) ?: items
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: layoutInflater.inflate(resource, parent, false)
        val text = view as TextView

        text.text = getItem(position).toString()

        return view
    }

    override fun getItem(position: Int): Any  = items[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getCount(): Int = items.size
    override fun getFilter(): Filter = filter
}

