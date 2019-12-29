package com.persidius.eos.aurora.ui.recipient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.persidius.eos.aurora.MainActivity
import com.persidius.eos.aurora.R
import com.persidius.eos.aurora.databinding.FragmentRecipientBinding

class RecipientFragment: Fragment() {
    companion object {
        const val ARG_RECIPIENT_ID = "recipientId"
        const val ARG_SESSION_ID = "sessionId"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Get the arguments
        val recipientId = arguments?.getString(ARG_RECIPIENT_ID)
        val sessionId = arguments?.getString(ARG_SESSION_ID)
        (activity as MainActivity).actionBar?.title = recipientId ?: "Recipient"

        // Generate the binder
        val binding: FragmentRecipientBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_recipient, container, false)
        binding.lifecycleOwner = this
        // TODO: Databinding crap.

        // LOADING:
        // 1. UAT List
        // 2. Recipient

        return binding.root
    }
}