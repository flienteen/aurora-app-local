package com.persidius.eos.aurora.ui.task

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.ui.recipient.RecipientFragment


class RecipientListAdapter(private val context: Context, private val recipients: MutableList<String>) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private var mainActivity: MainActivity = context as MainActivity

    override fun getCount(): Int {
        return recipients.size
    }

    override fun getItem(position: Int): String {
        return recipients[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = inflater.inflate(R.layout.recipient_list_item, parent, false)
        val txtRecipient = view.findViewById(R.id.txtRecipient) as TextView
        txtRecipient.text = getItem(position)
        val btnRecipient = view.findViewById(R.id.btnRemove) as ImageButton

        view.setOnClickListener {
            val args = Bundle()
            val recipient = getItem(position)
            args.putString(RecipientFragment.ARG_RECIPIENT_ID, recipient)
            mainActivity.navController.navigate(R.id.nav_recipient, args)
        }

        btnRecipient.setOnClickListener {
            recipients.removeAt(position)
            (parent as ListView).invalidateViews()
        }
        return view
    }

}