package com.persidius.eos.aurora.ui.util

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.Normalizer


class FoldingArrayAdapter(private val context: Context,
                          private val resource: Int,
                          private val items: List<String>
    ): BaseAdapter(), Filterable {

    private val layoutInflater = LayoutInflater.from(context)

    private val filter: AccentFoldArrayFilter = AccentFoldArrayFilter()
    private val rex1 = Regex("[ț]")
    private val rex2 = Regex("[ș]")
    private val rex3 = Regex("[î]")
    private val rex4 = Regex("[âă]")
    private val normalizedItems = items.map { i -> normalize(i) }
    private var showItems = items

    private fun normalize(s: String): String =
            s.toLowerCase()
            .replace(rex1, "t")
            .replace(rex2, "s")
            .replace(rex3, "i")
            .replace(rex4, "a")

    private fun filterItems(prefix: String): List<String> {
        val filteredItems = ArrayList<String>()
        for((index, value) in normalizedItems.withIndex()) {
            if(value.startsWith(prefix)) {
                filteredItems.add(items[index])
            } else {
                // take sublist firstIndex 1+ as
                // 1st word will always be 1st tried against
                val words = value.split(" ").drop(1)
                for(word in words) {
                    if(word.startsWith(prefix)) {
                        filteredItems.add(items[index])
                        break
                    }
                }
            }
        }

        return filteredItems
    }

    inner class Validator(val default: String): AutoCompleteTextView.Validator {

        override fun isValid(text: CharSequence?): Boolean = items.contains(text.toString())

        override fun fixText(invalidText: CharSequence?): CharSequence {
            // see if we can match it against anything

            val filteredItems = filterItems(invalidText.toString())
            // now iterate the FIs to see if there's any "normalized equals"
            val text = normalize(invalidText.toString())
            for(item in filteredItems) {
                if(text == normalize(item)) {
                    return item
                }
            }
            return default
        }
    }

    fun getValidator(defaultValue: String) = Validator(defaultValue)

    //// TEXT FILTERING
    private inner class AccentFoldArrayFilter: Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()

            if(constraint == null || constraint.isEmpty()) {
                // Stick all results on no constraints
                results.values = items
                results.count = items.size
            } else {
                val filteredItems = filterItems(constraint.toString())
                results.values = filteredItems
                results.count = filteredItems.size
            }
            return results
        }

        override fun publishResults(
            constraint: CharSequence?,
            results: FilterResults?
        ) {

            showItems = (results?.values as List<String>?) ?: items
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: layoutInflater.inflate(resource, parent, false)
        val text = view as TextView

        text.text = getItem(position).toString()

        return view
    }

    override fun getItem(position: Int): Any  = showItems[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getCount(): Int = showItems.size
    override fun getFilter(): Filter = filter
}

