package com.persidius.eos.aurora.ui.searchRecipient

import com.persidius.eos.aurora.database.entities.Recipient
import com.persidius.eos.aurora.util.GenericViewModelProviderFactory

class SearchRecipientViewModelProviderFactory(private val adapter: SearchRecipientAdapter):
    GenericViewModelProviderFactory<SearchRecipientViewModel>(SearchRecipientViewModel::class.java) {
    override fun onCreate() = SearchRecipientViewModel(adapter)
}